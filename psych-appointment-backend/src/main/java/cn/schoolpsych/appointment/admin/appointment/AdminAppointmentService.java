package cn.schoolpsych.appointment.admin.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.schoolpsych.appointment.admin.audit.AuditActions;
import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
import cn.schoolpsych.appointment.domain.appointment.AppointmentFormMetadata;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskAssessment;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.domain.appointment.RiskReviewStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskReviewMetadata;
import cn.schoolpsych.appointment.domain.appointment.RiskScreeningAnswers;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.domain.common.BaseEntity;
import cn.schoolpsych.appointment.domain.counselor.Counselor;
import cn.schoolpsych.appointment.domain.location.Campus;
import cn.schoolpsych.appointment.domain.location.Room;
import cn.schoolpsych.appointment.domain.referral.Referral;
import cn.schoolpsych.appointment.domain.schedule.AppointmentSlot;
import cn.schoolpsych.appointment.domain.service.ServiceType;
import cn.schoolpsych.appointment.domain.student.Student;
import cn.schoolpsych.appointment.repository.AppointmentFormRepository;
import cn.schoolpsych.appointment.repository.AppointmentRepository;
import cn.schoolpsych.appointment.repository.AppointmentSlotRepository;
import cn.schoolpsych.appointment.repository.CampusRepository;
import cn.schoolpsych.appointment.repository.CounselorRepository;
import cn.schoolpsych.appointment.repository.ReferralRepository;
import cn.schoolpsych.appointment.repository.RiskAssessmentRepository;
import cn.schoolpsych.appointment.repository.RoomRepository;
import cn.schoolpsych.appointment.repository.ServiceTypeRepository;
import cn.schoolpsych.appointment.repository.StudentRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import cn.schoolpsych.appointment.security.AppointmentSensitiveDataService;
import cn.schoolpsych.appointment.security.SensitiveDataEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentFormRepository formRepository;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final ReferralRepository referralRepository;
    private final AppointmentSlotRepository slotRepository;
    private final StudentRepository studentRepository;
    private final CounselorRepository counselorRepository;
    private final CampusRepository campusRepository;
    private final RoomRepository roomRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final SensitiveDataEncryptor sensitiveDataEncryptor;
    private final AppointmentSensitiveDataService appointmentSensitiveData;
    private final AuditLogService auditLogService;

    public AdminAppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentFormRepository formRepository,
            RiskAssessmentRepository riskAssessmentRepository,
            ReferralRepository referralRepository,
            AppointmentSlotRepository slotRepository,
            StudentRepository studentRepository,
            CounselorRepository counselorRepository,
            CampusRepository campusRepository,
            RoomRepository roomRepository,
            ServiceTypeRepository serviceTypeRepository,
            SensitiveDataEncryptor sensitiveDataEncryptor,
            AppointmentSensitiveDataService appointmentSensitiveData,
            AuditLogService auditLogService) {
        this.appointmentRepository = appointmentRepository;
        this.formRepository = formRepository;
        this.riskAssessmentRepository = riskAssessmentRepository;
        this.referralRepository = referralRepository;
        this.slotRepository = slotRepository;
        this.studentRepository = studentRepository;
        this.counselorRepository = counselorRepository;
        this.campusRepository = campusRepository;
        this.roomRepository = roomRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.sensitiveDataEncryptor = sensitiveDataEncryptor;
        this.appointmentSensitiveData = appointmentSensitiveData;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AdminAppointmentRecordResponse> list(
            AppointmentStatus status,
            RiskLevel riskLevel,
            Long studentId,
            String studentNo,
            Long counselorId,
            LocalDate from,
            LocalDate to) {
        Long resolvedStudentId = resolveStudentId(studentId, studentNo);
        DateRange range = toDateRange(from, to);
        List<Appointment> appointments = appointmentRepository.findAdminAppointments(
                status,
                riskLevel,
                resolvedStudentId,
                counselorId,
                range.fromAt(),
                range.toAt());
        RelatedData relatedData = relatedData(appointments);
        Map<Long, RiskAssessment> riskByAppointmentId = riskByAppointmentId(appointments);
        return appointments.stream()
                .map(appointment -> toRecord(appointment, relatedData, riskByAppointmentId.get(appointment.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminAppointmentDetailResponse detail(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        RelatedData relatedData = relatedData(List.of(appointment));
        AppointmentForm form = formRepository.findByAppointmentId(appointment.getId()).orElse(null);
        RiskAssessment risk = riskAssessmentRepository.findByAppointmentId(appointment.getId()).orElse(null);
        Referral referral = referralRepository.findByAppointmentId(appointment.getId()).orElse(null);
        Student student = relatedData.students().get(appointment.getStudentId());
        Counselor counselor = relatedData.counselors().get(appointment.getCounselorId());
        Campus campus = relatedData.campuses().get(appointment.getCampusId());
        Room room = appointment.getRoomId() == null ? null : relatedData.rooms().get(appointment.getRoomId());
        ServiceType serviceType = relatedData.serviceTypes().get(appointment.getServiceTypeId());
        AppointmentFormMetadata formMetadata = appointmentSensitiveData.readFormMetadata(form);
        RiskScreeningAnswers riskAnswers = risk == null
                ? new RiskScreeningAnswers(false, false, false, false, false, false)
                : appointmentSensitiveData.readRiskAnswers(risk);
        RiskReviewMetadata reviewMetadata = risk == null
                ? new RiskReviewMetadata(null, null)
                : appointmentSensitiveData.readReviewMetadata(risk);
        return new AdminAppointmentDetailResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                appointment.getRiskLevel(),
                appointment.getStudentId(),
                student == null ? null : student.getStudentNo(),
                student == null ? null : student.getName(),
                student == null ? null : student.getGender(),
                student == null ? null : student.getCollege(),
                student == null ? null : student.getMajor(),
                student == null ? null : student.getGrade(),
                student == null ? null : student.getClassName(),
                appointment.getCounselorId(),
                counselor == null ? null : counselor.getName(),
                counselor == null ? null : counselor.getTitle(),
                appointment.getCampusId(),
                campus == null ? null : campus.getName(),
                appointment.getRoomId(),
                room == null ? null : room.getName(),
                appointment.getServiceTypeId(),
                serviceType == null ? null : serviceType.getName(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                form != null && form.isFirstVisit(),
                formMetadata.issueTypes(),
                formMetadata.urgencyLevel(),
                formMetadata.contactTime(),
                riskAnswers.selfHarm(),
                riskAnswers.harmOthers(),
                riskAnswers.crisisEvent(),
                riskAnswers.psychiatricTreatment(),
                riskAnswers.medication(),
                riskAnswers.willingContact(),
                risk == null ? null : risk.getReviewStatus(),
                reviewMetadata.reviewedBy(),
                reviewMetadata.reviewedAt(),
                referral == null ? null : referral.getId(),
                referral == null ? null : referral.getReferralType(),
                appointmentSensitiveData.readReferralDestination(referral),
                referral == null ? null : referral.getStatus(),
                appointmentSensitiveData.readCancellationReason(appointment),
                appointment.getCanceledAt(),
                appointment.getCompletedAt());
    }

    @Transactional
    public AdminCancelAppointmentResponse cancel(
            Long appointmentId,
            AdminCancelAppointmentRequest request,
            AuthenticatedAccount principal,
            AuditRequestMetadata metadata) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!appointment.canBeCanceledByAdmin()) {
            throw new IllegalArgumentException("Appointment cannot be canceled by admin");
        }
        AppointmentSlot slot = slotRepository.findByIdForUpdate(appointment.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));
        appointment.cancelByAdmin(principal.accountId(), appointmentSensitiveData.encryptText(request.reason()));
        slot.releaseBooking(appointment.getId());
        appointmentRepository.save(appointment);
        slotRepository.save(slot);
        AdminCancelAppointmentResponse response = new AdminCancelAppointmentResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                slot.getId(),
                slot.getStatus(),
                appointment.getCanceledAt());
        auditLogService.record(
                principal,
                AuditActions.APPOINTMENT_CANCELED,
                "APPOINTMENT",
                appointment.getId(),
                SensitiveLevel.SENSITIVE,
                metadata,
                Map.of(
                        "appointmentNo", appointment.getAppointmentNo(),
                        "status", appointment.getStatus().name(),
                        "reasonProvided", request.reason() != null && !request.reason().isBlank()));
        return response;
    }

    @Transactional
    public RiskReviewResponse reviewRisk(
            Long appointmentId,
            RiskReviewRequest request,
            AuthenticatedAccount principal,
            AuditRequestMetadata metadata) {
        Appointment appointment = appointmentRepository.findByIdForUpdate(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!appointment.canBeRiskReviewed()) {
            throw new IllegalArgumentException("Appointment is not pending high-risk review");
        }
        RiskAssessment risk = riskAssessmentRepository.findByAppointmentId(appointment.getId())
                .orElseThrow(() -> new IllegalArgumentException("Risk assessment not found"));
        if (risk.getReviewStatus() != RiskReviewStatus.PENDING) {
            throw new IllegalArgumentException("Risk assessment is not pending review");
        }
        AppointmentSlot slot = slotRepository.findByIdForUpdate(appointment.getSlotId())
                .orElseThrow(() -> new IllegalArgumentException("Slot not found"));

        Referral referral = null;
        byte[] handlingNotes = sensitiveDataEncryptor.encryptNullable(request.handlingNotes());
        LocalDateTime reviewedAt = LocalDateTime.now();
        byte[] reviewMetadata = appointmentSensitiveData.encryptReviewMetadata(principal.accountId(), reviewedAt);
        switch (request.decision()) {
            case APPROVE -> {
                appointment.approveRiskReview();
                risk.review(RiskReviewStatus.APPROVED, handlingNotes, reviewMetadata);
            }
            case REFER -> {
                requireReferralFields(request);
                appointment.referAfterRiskReview();
                risk.review(RiskReviewStatus.REFERRED, handlingNotes, reviewMetadata);
                referral = referralRepository.save(Referral.open(
                        appointment.getId(),
                        appointment.getStudentId(),
                        appointment.getCounselorId(),
                        request.referralType(),
                        appointmentSensitiveData.encryptText(request.referralDestination()),
                        sensitiveDataEncryptor.encryptNullable(request.referralReason())));
                slot.releaseBooking(appointment.getId());
            }
            case CLOSE -> {
                appointment.closeAfterRiskReview();
                risk.review(RiskReviewStatus.CLOSED, handlingNotes, reviewMetadata);
                slot.releaseBooking(appointment.getId());
            }
        }
        appointmentRepository.save(appointment);
        riskAssessmentRepository.save(risk);
        slotRepository.save(slot);
        RiskReviewResponse response = new RiskReviewResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                risk.getReviewStatus(),
                principal.accountId(),
                reviewedAt,
                referral == null ? null : referral.getId(),
                referral == null ? null : referral.getReferralType(),
                appointmentSensitiveData.readReferralDestination(referral),
                referral == null ? null : referral.getStatus(),
                slot.getId(),
                slot.getStatus());
        auditLogService.record(
                principal,
                AuditActions.RISK_REVIEWED,
                "APPOINTMENT",
                appointment.getId(),
                SensitiveLevel.HIGH,
                metadata,
                Map.of(
                        "decision", request.decision().name(),
                        "appointmentStatus", appointment.getStatus().name(),
                        "riskReviewStatus", risk.getReviewStatus().name(),
                        "referralCreated", referral != null));
        return response;
    }

    private AdminAppointmentRecordResponse toRecord(
            Appointment appointment,
            RelatedData relatedData,
            RiskAssessment risk) {
        Student student = relatedData.students().get(appointment.getStudentId());
        Counselor counselor = relatedData.counselors().get(appointment.getCounselorId());
        Campus campus = relatedData.campuses().get(appointment.getCampusId());
        Room room = appointment.getRoomId() == null ? null : relatedData.rooms().get(appointment.getRoomId());
        ServiceType serviceType = relatedData.serviceTypes().get(appointment.getServiceTypeId());
        return new AdminAppointmentRecordResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                appointment.getRiskLevel(),
                risk == null ? null : risk.getReviewStatus(),
                appointment.getStudentId(),
                student == null ? null : student.getStudentNo(),
                student == null ? null : student.getName(),
                student == null ? null : student.getCollege(),
                student == null ? null : student.getGrade(),
                appointment.getCounselorId(),
                counselor == null ? null : counselor.getName(),
                appointment.getCampusId(),
                campus == null ? null : campus.getName(),
                appointment.getRoomId(),
                room == null ? null : room.getName(),
                appointment.getServiceTypeId(),
                serviceType == null ? null : serviceType.getName(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getCanceledAt(),
                appointment.getCompletedAt());
    }

    private RelatedData relatedData(List<Appointment> appointments) {
        return new RelatedData(
                mapById(studentRepository.findAllById(ids(appointments, Appointment::getStudentId))),
                mapById(counselorRepository.findAllById(ids(appointments, Appointment::getCounselorId))),
                mapById(campusRepository.findAllById(ids(appointments, Appointment::getCampusId))),
                mapById(roomRepository.findAllById(ids(appointments, Appointment::getRoomId))),
                mapById(serviceTypeRepository.findAllById(ids(appointments, Appointment::getServiceTypeId))));
    }

    private Map<Long, RiskAssessment> riskByAppointmentId(List<Appointment> appointments) {
        Map<Long, RiskAssessment> result = new HashMap<>();
        riskAssessmentRepository.findByAppointmentIdIn(ids(appointments, Appointment::getId))
                .forEach(risk -> result.put(risk.getAppointmentId(), risk));
        return result;
    }

    private Long resolveStudentId(Long studentId, String studentNo) {
        if (studentId != null) {
            return studentId;
        }
        if (studentNo == null || studentNo.isBlank()) {
            return null;
        }
        return studentRepository.findByStudentNo(studentNo.trim())
                .map(Student::getId)
                .orElse(-1L);
    }

    private void requireReferralFields(RiskReviewRequest request) {
        if (request.referralType() == null) {
            throw new IllegalArgumentException("referralType is required for REFER decision");
        }
        if (request.referralDestination() == null || request.referralDestination().isBlank()) {
            throw new IllegalArgumentException("referralDestination is required for REFER decision");
        }
        if (request.referralReason() == null || request.referralReason().isBlank()) {
            throw new IllegalArgumentException("referralReason is required for REFER decision");
        }
    }

    private List<Long> ids(List<Appointment> appointments, java.util.function.Function<Appointment, Long> getter) {
        return appointments.stream()
                .map(getter)
                .filter(id -> id != null)
                .distinct()
                .toList();
    }

    private <T extends BaseEntity> Map<Long, T> mapById(Iterable<T> entities) {
        Map<Long, T> result = new HashMap<>();
        for (T entity : entities) {
            result.put(entity.getId(), entity);
        }
        return result;
    }

    private DateRange toDateRange(LocalDate from, LocalDate to) {
        LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
        LocalDateTime toAt = to == null ? null : to.plusDays(1).atStartOfDay();
        if (fromAt != null && toAt != null && !toAt.isAfter(fromAt)) {
            throw new IllegalArgumentException("to must not be before from");
        }
        return new DateRange(fromAt, toAt);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DateRange(LocalDateTime fromAt, LocalDateTime toAt) {
    }

    private record RelatedData(
            Map<Long, Student> students,
            Map<Long, Counselor> counselors,
            Map<Long, Campus> campuses,
            Map<Long, Room> rooms,
            Map<Long, ServiceType> serviceTypes) {
    }
}
