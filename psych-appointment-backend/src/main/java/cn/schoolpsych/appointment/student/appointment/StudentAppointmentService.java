package cn.schoolpsych.appointment.student.appointment;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
import cn.schoolpsych.appointment.domain.appointment.AppointmentSlotSnapshot;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskAssessment;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.domain.appointment.RiskScreeningAnswers;
import cn.schoolpsych.appointment.domain.consent.ConsentRecord;
import cn.schoolpsych.appointment.domain.consent.ConsentVersion;
import cn.schoolpsych.appointment.domain.rule.AppointmentRuleSet;
import cn.schoolpsych.appointment.domain.rule.SchoolTerm;
import cn.schoolpsych.appointment.domain.schedule.AppointmentSlot;
import cn.schoolpsych.appointment.domain.schedule.SlotStatus;
import cn.schoolpsych.appointment.domain.student.Student;
import cn.schoolpsych.appointment.repository.AppointmentFormRepository;
import cn.schoolpsych.appointment.repository.AppointmentRepository;
import cn.schoolpsych.appointment.repository.AppointmentRuleSetRepository;
import cn.schoolpsych.appointment.repository.AppointmentSlotRepository;
import cn.schoolpsych.appointment.repository.ConsentRecordRepository;
import cn.schoolpsych.appointment.repository.ConsentVersionRepository;
import cn.schoolpsych.appointment.repository.RiskAssessmentRepository;
import cn.schoolpsych.appointment.repository.SchoolTermRepository;
import cn.schoolpsych.appointment.repository.StudentRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import cn.schoolpsych.appointment.security.AppointmentSensitiveDataService;
import cn.schoolpsych.appointment.security.SensitiveDataEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAppointmentService {

    private static final int DEFAULT_SLOT_LOCK_MINUTES = 10;
    private static final int DEFAULT_MIN_BOOKING_HOURS_AHEAD = 24;
    private static final int DEFAULT_MAX_BOOKING_DAYS_AHEAD = 14;
    private static final int DEFAULT_MAX_ACTIVE_APPOINTMENTS = 1;
    private static final int DEFAULT_MAX_WEEKLY_APPOINTMENTS = 1;
    private static final int DEFAULT_MAX_SEMESTER_COMPLETED_APPOINTMENTS = 8;
    private static final int DEFAULT_NO_SHOW_RESTRICT_THRESHOLD = 2;

    private static final List<AppointmentStatus> ACTIVE_STATUSES = List.of(
            AppointmentStatus.SUBMITTED,
            AppointmentStatus.RISK_REVIEW,
            AppointmentStatus.COUNSELOR_REVIEW,
            AppointmentStatus.ADMIN_REVIEW,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.CHECKED_IN);

    private static final List<AppointmentStatus> BOOKING_LIMIT_STATUSES = List.of(
            AppointmentStatus.SUBMITTED,
            AppointmentStatus.RISK_REVIEW,
            AppointmentStatus.COUNSELOR_REVIEW,
            AppointmentStatus.ADMIN_REVIEW,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.CHECKED_IN,
            AppointmentStatus.NO_SHOW,
            AppointmentStatus.COMPLETED);

    private static final Set<String> SUPPORTED_ISSUE_TYPES = Set.of(
            "academic-stress",
            "emotion",
            "relationship",
            "family",
            "sleep",
            "career");

    private final StudentRepository studentRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentFormRepository formRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final ConsentVersionRepository consentVersionRepository;
    private final ConsentRecordRepository consentRecordRepository;
    private final AppointmentRuleSetRepository ruleSetRepository;
    private final SchoolTermRepository schoolTermRepository;
    private final SensitiveDataEncryptor sensitiveDataEncryptor;
    private final AppointmentSensitiveDataService appointmentSensitiveData;
    private final ObjectMapper objectMapper;
    private final StudentCancellationPolicy cancellationPolicy;

    public StudentAppointmentService(
            StudentRepository studentRepository,
            AppointmentSlotRepository slotRepository,
            AppointmentRepository appointmentRepository,
            AppointmentFormRepository formRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            ConsentVersionRepository consentVersionRepository,
            ConsentRecordRepository consentRecordRepository,
            AppointmentRuleSetRepository ruleSetRepository,
            SchoolTermRepository schoolTermRepository,
            SensitiveDataEncryptor sensitiveDataEncryptor,
            AppointmentSensitiveDataService appointmentSensitiveData,
            ObjectMapper objectMapper,
            StudentCancellationPolicy cancellationPolicy) {
        this.studentRepository = studentRepository;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.formRepository = formRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.consentVersionRepository = consentVersionRepository;
        this.consentRecordRepository = consentRecordRepository;
        this.ruleSetRepository = ruleSetRepository;
        this.schoolTermRepository = schoolTermRepository;
        this.sensitiveDataEncryptor = sensitiveDataEncryptor;
        this.appointmentSensitiveData = appointmentSensitiveData;
        this.objectMapper = objectMapper;
        this.cancellationPolicy = cancellationPolicy;
    }

    @Transactional
    public LockSlotResponse lockSlot(Long slotId, AuthenticatedAccount principal) {
        LocalDateTime now = LocalDateTime.now();
        slotRepository.releaseExpiredLocks(now);
        AppointmentRuleSet ruleSet = activeRuleSet();
        JsonNode settings = readSettings(ruleSet.getSettingsJson()).orElseGet(objectMapper::createObjectNode);
        Student student = lockedStudent(principal.accountId());
        ensureStudentCanBook(student, now, settings);
        ensureActiveAppointmentLimit(student.getId(), settings, now);

        releaseStudentLocks(student.getId(), slotId);
        AppointmentSlot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (!slot.getStartAt().isAfter(now)) {
            throw new IllegalArgumentException("Slot has already started");
        }
        ensureSlotWithinBookingWindow(slot, now, settings);
        ensureWeeklyAppointmentLimit(student.getId(), slot.getStartAt(), settings);
        ensureSemesterCompletedLimit(student.getId(), currentSchoolTerm().getId(), settings);
        if (!slot.canBeLockedBy(student.getId(), now)) {
            throw new IllegalArgumentException("Slot is not available");
        }

        slot.markLocked(student.getId(), now.plusMinutes(slotLockMinutes(settings)));
        slotRepository.save(slot);
        return new LockSlotResponse(
                slot.getId(),
                slot.getCounselorId(),
                slot.getStartAt(),
                slot.getEndAt(),
                slot.getStatus(),
                slot.getLockedUntil());
    }

    @Transactional
    public SubmitAppointmentResponse submit(SubmitAppointmentRequest request, AuthenticatedAccount principal, String clientInfo) {
        LocalDateTime now = LocalDateTime.now();
        if (!request.consentAgreed()) {
            throw new IllegalArgumentException("Consent must be agreed before submitting appointment");
        }
        ensureSupportedIssueTypes(request.issueTypes());

        AppointmentRuleSet ruleSet = activeRuleSet();
        JsonNode settings = readSettings(ruleSet.getSettingsJson()).orElseGet(objectMapper::createObjectNode);
        Student student = lockedStudent(principal.accountId());
        ensureStudentCanBook(student, now, settings);
        ensureActiveAppointmentLimit(student.getId(), settings, now);

        AppointmentSlot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        if (!slot.canBeBookedBy(student.getId(), now)) {
            throw new IllegalArgumentException("Slot is not available for this student");
        }
        if (!slot.getStartAt().isAfter(now)) {
            throw new IllegalArgumentException("Slot has already started");
        }
        ensureSlotWithinBookingWindow(slot, now, settings);
        ensureWeeklyAppointmentLimit(student.getId(), slot.getStartAt(), settings);

        ConsentVersion consentVersion = consentVersionRepository.findFirstByStatusOrderByPublishedAtDesc("PUBLISHED")
                .orElseThrow(() -> new IllegalArgumentException("No published consent version"));
        if (!consentVersion.getId().equals(request.consentVersionId())) {
            throw new IllegalArgumentException("Consent version has changed; please review the latest version");
        }
        SchoolTerm term = currentSchoolTerm();
        ensureSemesterCompletedLimit(student.getId(), term.getId(), settings);

        RiskLevel riskLevel = evaluateRisk(request.urgencyLevel(), request.risk());
        AppointmentStatus status = riskLevel == RiskLevel.HIGH ? AppointmentStatus.RISK_REVIEW : AppointmentStatus.CONFIRMED;

        ConsentRecord consentRecord = consentRecordRepository.save(ConsentRecord.create(student.getId(), consentVersion.getId(), clientInfo));
        Appointment appointment = appointmentRepository.save(Appointment.create(
                nextAppointmentNo(),
                student.getId(),
                AppointmentSlotSnapshot.from(slot),
                term.getId(),
                ruleSet.getId(),
                consentRecord.getId(),
                status,
                riskLevel));

        slot.markBooked(appointment.getId());
        slotRepository.save(slot);
        releaseStudentLocks(student.getId(), slot.getId());

        formRepository.save(AppointmentForm.create(
                appointment.getId(),
                request.firstVisit(),
                appointmentSensitiveData.encryptFormMetadata(
                        request.issueTypes(), request.urgencyLevel(), request.contactTime()),
                sensitiveDataEncryptor.encryptNullable(request.description()),
                sensitiveDataEncryptor.encryptNullable(request.expectedHelp())));

        riskAssessmentRepository.save(RiskAssessment.create(
                appointment.getId(),
                appointmentSensitiveData.encryptRiskAnswers(new RiskScreeningAnswers(
                        request.risk().selfHarm(),
                        request.risk().harmOthers(),
                        request.risk().crisisEvent(),
                        request.risk().psychiatricTreatment(),
                        request.risk().medication(),
                        request.risk().willingContact())),
                riskLevel));

        return new SubmitAppointmentResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                appointment.getRiskLevel(),
                slot.getId(),
                slot.getCounselorId(),
                slot.getStartAt(),
                slot.getEndAt());
    }

    @Transactional
    public CancelAppointmentResponse cancel(
            Long appointmentId,
            CancelAppointmentRequest request,
            AuthenticatedAccount principal) {
        LocalDateTime now = LocalDateTime.now();
        Student student = lockedStudent(principal.accountId());
        Appointment appointment = appointmentRepository.findByIdAndStudentIdForUpdate(appointmentId, student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!appointment.canBeCanceledByStudent(now)) {
            throw new IllegalArgumentException("Appointment cannot be canceled by student");
        }
        StudentCancellationPolicy.CancellationAvailability cancellation = cancellationPolicy.evaluate(appointment, now);
        if (!cancellation.canCancel()) {
            throw new IllegalArgumentException(
                    "Appointment cannot be canceled within "
                            + cancellation.minCancelHoursAhead()
                            + " hours before start");
        }

        AppointmentSlot slot = slotRepository.findByIdForUpdate(appointment.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        appointment.cancelByStudent(principal.accountId(), appointmentSensitiveData.encryptText(request.reason()));
        slot.releaseBooking(appointment.getId());
        appointmentRepository.save(appointment);
        slotRepository.save(slot);
        return new CancelAppointmentResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                slot.getId(),
                slot.getStatus(),
                appointment.getCanceledAt());
    }

    private Student lockedStudent(Long accountId) {
        return studentRepository.findByAccountIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Student account not found"));
    }

    private void ensureStudentCanBook(Student student, LocalDateTime now, JsonNode settings) {
        int noShowRestrictThreshold = settings.path("noShowRestrictThreshold")
                .asInt(DEFAULT_NO_SHOW_RESTRICT_THRESHOLD);
        if (!student.canBookNow(now, noShowRestrictThreshold)) {
            throw new IllegalArgumentException("Student is not allowed to book appointments");
        }
    }

    private void ensureActiveAppointmentLimit(Long studentId, JsonNode settings, LocalDateTime now) {
        int maxActiveAppointments = settings.path("maxActiveAppointments").asInt(DEFAULT_MAX_ACTIVE_APPOINTMENTS);
        if (appointmentRepository.countByStudentIdAndStatusInAndEndAtAfter(studentId, ACTIVE_STATUSES, now)
                >= maxActiveAppointments) {
            throw new IllegalArgumentException("Student has reached the active appointment limit");
        }
    }

    private void ensureWeeklyAppointmentLimit(Long studentId, LocalDateTime slotStartAt, JsonNode settings) {
        int maxWeeklyAppointments = settings.path("maxWeeklyAppointments").asInt(DEFAULT_MAX_WEEKLY_APPOINTMENTS);
        LocalDate weekStart = slotStartAt.toLocalDate()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long appointmentCount = appointmentRepository
                .countByStudentIdAndStatusInAndStartAtGreaterThanEqualAndStartAtLessThan(
                        studentId,
                        BOOKING_LIMIT_STATUSES,
                        weekStart.atStartOfDay(),
                        weekStart.plusWeeks(1).atStartOfDay());
        if (appointmentCount >= maxWeeklyAppointments) {
            throw new IllegalArgumentException("Student has reached the weekly appointment limit");
        }
    }

    private void ensureSemesterCompletedLimit(Long studentId, Long semesterId, JsonNode settings) {
        int maxCompleted = settings.path("maxSemesterCompletedAppointments")
                .asInt(DEFAULT_MAX_SEMESTER_COMPLETED_APPOINTMENTS);
        long completedCount = appointmentRepository.countByStudentIdAndSemesterIdAndStatus(
                studentId, semesterId, AppointmentStatus.COMPLETED);
        if (completedCount >= maxCompleted) {
            throw new IllegalArgumentException("Student has reached the semester completed appointment limit");
        }
    }

    private void ensureSlotWithinBookingWindow(AppointmentSlot slot, LocalDateTime now, JsonNode settings) {
        int minHoursAhead = settings.path("minBookingHoursAhead").asInt(DEFAULT_MIN_BOOKING_HOURS_AHEAD);
        int maxDaysAhead = settings.path("maxBookingDaysAhead").asInt(DEFAULT_MAX_BOOKING_DAYS_AHEAD);
        if (slot.getStartAt().isBefore(now.plusHours(minHoursAhead))) {
            throw new IllegalArgumentException("Slot is earlier than the minimum booking lead time");
        }
        if (slot.getStartAt().isAfter(now.plusDays(maxDaysAhead))) {
            throw new IllegalArgumentException("Slot is beyond the maximum booking window");
        }
    }

    private AppointmentRuleSet activeRuleSet() {
        return ruleSetRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()
                .orElseThrow(() -> new IllegalArgumentException("No active appointment rule set"));
    }

    private SchoolTerm currentSchoolTerm() {
        return schoolTermRepository.findFirstByCurrentTrueAndStatus("ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException("No active school term"));
    }

    private void releaseStudentLocks(Long studentId, Long exceptSlotId) {
        List<AppointmentSlot> lockedSlots = slotRepository.findByLockedByStudentIdAndStatus(studentId, SlotStatus.LOCKED);
        for (AppointmentSlot lockedSlot : lockedSlots) {
            if (exceptSlotId == null || !exceptSlotId.equals(lockedSlot.getId())) {
                lockedSlot.releaseLock();
            }
        }
        slotRepository.saveAll(lockedSlots);
    }

    private RiskLevel evaluateRisk(RiskLevel urgencyLevel, RiskScreeningRequest risk) {
        if (risk.selfHarm() || risk.harmOthers() || risk.crisisEvent() || urgencyLevel == RiskLevel.HIGH) {
            return RiskLevel.HIGH;
        }
        if (risk.psychiatricTreatment() || risk.medication() || urgencyLevel == RiskLevel.MEDIUM) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    static void ensureSupportedIssueTypes(List<String> issueTypes) {
        for (String issueType : issueTypes) {
            if (!SUPPORTED_ISSUE_TYPES.contains(issueType)) {
                throw new IllegalArgumentException("Unsupported appointment issue type: " + issueType);
            }
        }
    }

    private String nextAppointmentNo() {
        return "APT"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + ThreadLocalRandom.current().nextInt(1000, 10000);
    }

    private int slotLockMinutes(JsonNode settings) {
        return settings.path("slotLockMinutes").asInt(DEFAULT_SLOT_LOCK_MINUTES);
    }

    private Optional<JsonNode> readSettings(String settingsJson) {
        try {
            return Optional.of(objectMapper.readTree(settingsJson));
        } catch (JsonProcessingException exception) {
            return Optional.empty();
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
