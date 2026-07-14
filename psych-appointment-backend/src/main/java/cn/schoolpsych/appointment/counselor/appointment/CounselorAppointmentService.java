package cn.schoolpsych.appointment.counselor.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.admin.audit.AuditActions;
import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
import cn.schoolpsych.appointment.domain.appointment.AppointmentFormMetadata;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.common.BaseEntity;
import cn.schoolpsych.appointment.domain.counselor.Counselor;
import cn.schoolpsych.appointment.domain.location.Campus;
import cn.schoolpsych.appointment.domain.note.ConsultationNote;
import cn.schoolpsych.appointment.domain.location.Room;
import cn.schoolpsych.appointment.domain.service.ServiceType;
import cn.schoolpsych.appointment.domain.student.Student;
import cn.schoolpsych.appointment.repository.AppointmentFormRepository;
import cn.schoolpsych.appointment.repository.AppointmentRepository;
import cn.schoolpsych.appointment.repository.CampusRepository;
import cn.schoolpsych.appointment.repository.ConsultationNoteRepository;
import cn.schoolpsych.appointment.repository.CounselorRepository;
import cn.schoolpsych.appointment.repository.RoomRepository;
import cn.schoolpsych.appointment.repository.ServiceTypeRepository;
import cn.schoolpsych.appointment.repository.StudentRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import cn.schoolpsych.appointment.security.AppointmentSensitiveDataService;
import cn.schoolpsych.appointment.security.SensitiveDataEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounselorAppointmentService {

    private final CounselorRepository counselorRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentFormRepository formRepository;
    private final ConsultationNoteRepository consultationNoteRepository;
    private final StudentRepository studentRepository;
    private final CampusRepository campusRepository;
    private final RoomRepository roomRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final SensitiveDataEncryptor sensitiveDataEncryptor;
    private final AppointmentSensitiveDataService appointmentSensitiveData;
    private final AuditLogService auditLogService;

    public CounselorAppointmentService(
            CounselorRepository counselorRepository,
            AppointmentRepository appointmentRepository,
            AppointmentFormRepository formRepository,
            ConsultationNoteRepository consultationNoteRepository,
            StudentRepository studentRepository,
            CampusRepository campusRepository,
            RoomRepository roomRepository,
            ServiceTypeRepository serviceTypeRepository,
            SensitiveDataEncryptor sensitiveDataEncryptor,
            AppointmentSensitiveDataService appointmentSensitiveData,
            AuditLogService auditLogService) {
        this.counselorRepository = counselorRepository;
        this.appointmentRepository = appointmentRepository;
        this.formRepository = formRepository;
        this.consultationNoteRepository = consultationNoteRepository;
        this.studentRepository = studentRepository;
        this.campusRepository = campusRepository;
        this.roomRepository = roomRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.sensitiveDataEncryptor = sensitiveDataEncryptor;
        this.appointmentSensitiveData = appointmentSensitiveData;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<CounselorAppointmentRecordResponse> list(
            AuthenticatedAccount principal,
            AppointmentStatus status,
            LocalDate from,
            LocalDate to) {
        Counselor counselor = counselorRepository.findByAccountId(principal.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Counselor account not found"));
        DateRange range = toDateRange(from, to);
        List<Appointment> appointments = appointmentRepository.findCounselorAppointments(
                counselor.getId(),
                status,
                range.fromAt(),
                range.toAt());
        RelatedData relatedData = relatedData(appointments);
        return appointments.stream()
                .map(appointment -> toRecord(appointment, relatedData))
                .toList();
    }

    @Transactional(readOnly = true)
    public CounselorAppointmentDetailResponse detail(Long appointmentId, AuthenticatedAccount principal) {
        Counselor counselor = counselorRepository.findByAccountId(principal.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Counselor account not found"));
        Appointment appointment = appointmentRepository.findByIdAndCounselorId(appointmentId, counselor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        RelatedData relatedData = relatedData(List.of(appointment));
        AppointmentForm form = formRepository.findByAppointmentId(appointment.getId()).orElse(null);
        Student student = relatedData.students().get(appointment.getStudentId());
        Campus campus = relatedData.campuses().get(appointment.getCampusId());
        Room room = appointment.getRoomId() == null ? null : relatedData.rooms().get(appointment.getRoomId());
        ServiceType serviceType = relatedData.serviceTypes().get(appointment.getServiceTypeId());
        AppointmentFormMetadata formMetadata = appointmentSensitiveData.readFormMetadata(form);
        return new CounselorAppointmentDetailResponse(
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
                appointmentSensitiveData.readCancellationReason(appointment),
                appointment.getCanceledAt());
    }

    @Transactional
    public CompleteAppointmentResponse complete(
            Long appointmentId,
            CompleteAppointmentRequest request,
            AuthenticatedAccount principal,
            AuditRequestMetadata metadata) {
        Counselor counselor = counselorRepository.findByAccountId(principal.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Counselor account not found"));
        Appointment appointment = appointmentRepository.findByIdAndCounselorIdForUpdate(appointmentId, counselor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        LocalDateTime now = LocalDateTime.now();
        if (!appointment.canBeCompletedByCounselor(now)) {
            throw new IllegalArgumentException("Appointment cannot be completed");
        }
        if (consultationNoteRepository.existsByAppointmentId(appointment.getId())) {
            throw new IllegalArgumentException("Consultation note already exists");
        }

        ConsultationNote note = consultationNoteRepository.save(ConsultationNote.submit(
                appointment.getId(),
                appointment.getStudentId(),
                counselor.getId(),
                sensitiveDataEncryptor.encryptNullable(request.topic()),
                sensitiveDataEncryptor.encryptNullable(request.summary()),
                normalizeRiskChange(request.riskChange()),
                sensitiveDataEncryptor.encryptNullable(request.followUpPlan()),
                request.needReferral()));
        appointment.complete(now);
        appointmentRepository.save(appointment);
        Map<String, Object> auditDetail = new HashMap<>();
        auditDetail.put("appointmentNo", appointment.getAppointmentNo());
        auditDetail.put("status", appointment.getStatus().name());
        auditDetail.put("noteId", note.getId());
        auditDetail.put("noteStatus", note.getStatus().name());
        auditDetail.put("riskChange", note.getRiskChange());
        auditDetail.put("needReferral", note.isNeedReferral());
        auditDetail.put("completedAt", appointment.getCompletedAt());
        auditLogService.record(
                principal,
                AuditActions.APPOINTMENT_COMPLETED,
                "APPOINTMENT",
                appointment.getId(),
                SensitiveLevel.SENSITIVE,
                metadata,
                auditDetail);
        return new CompleteAppointmentResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                appointment.getCompletedAt(),
                note.getId(),
                note.getStatus(),
                note.getRiskChange(),
                note.isNeedReferral());
    }

    @Transactional
    public MarkNoShowResponse markNoShow(
            Long appointmentId,
            AuthenticatedAccount principal,
            AuditRequestMetadata metadata) {
        Counselor counselor = counselorRepository.findByAccountId(principal.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Counselor account not found"));
        Appointment appointment = appointmentRepository.findByIdAndCounselorIdForUpdate(
                        appointmentId, counselor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        LocalDateTime now = LocalDateTime.now();
        if (!appointment.canBeMarkedNoShow(now)) {
            throw new IllegalArgumentException("Only ended confirmed appointments can be marked as no-show");
        }
        Student student = studentRepository.findByIdForUpdate(appointment.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        appointment.markNoShow(now);
        student.recordNoShow();
        appointmentRepository.save(appointment);
        studentRepository.save(student);
        auditLogService.record(
                principal,
                AuditActions.APPOINTMENT_MARKED_NO_SHOW,
                "APPOINTMENT",
                appointment.getId(),
                SensitiveLevel.SENSITIVE,
                metadata,
                Map.of(
                        "appointmentNo", appointment.getAppointmentNo(),
                        "status", appointment.getStatus().name(),
                        "studentNoShowCount", student.getNoShowCount()));
        return new MarkNoShowResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                student.getNoShowCount());
    }

    private CounselorAppointmentRecordResponse toRecord(Appointment appointment, RelatedData relatedData) {
        Student student = relatedData.students().get(appointment.getStudentId());
        Campus campus = relatedData.campuses().get(appointment.getCampusId());
        Room room = appointment.getRoomId() == null ? null : relatedData.rooms().get(appointment.getRoomId());
        ServiceType serviceType = relatedData.serviceTypes().get(appointment.getServiceTypeId());
        return new CounselorAppointmentRecordResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                appointment.getRiskLevel(),
                appointment.getStudentId(),
                student == null ? null : student.getStudentNo(),
                student == null ? null : student.getName(),
                student == null ? null : student.getCollege(),
                student == null ? null : student.getGrade(),
                student == null ? null : student.getClassName(),
                appointment.getCampusId(),
                campus == null ? null : campus.getName(),
                appointment.getRoomId(),
                room == null ? null : room.getName(),
                appointment.getServiceTypeId(),
                serviceType == null ? null : serviceType.getName(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointmentSensitiveData.readCancellationReason(appointment),
                appointment.getCanceledAt());
    }

    private RelatedData relatedData(List<Appointment> appointments) {
        return new RelatedData(
                mapById(studentRepository.findAllById(ids(appointments, Appointment::getStudentId))),
                mapById(campusRepository.findAllById(ids(appointments, Appointment::getCampusId))),
                mapById(roomRepository.findAllById(ids(appointments, Appointment::getRoomId))),
                mapById(serviceTypeRepository.findAllById(ids(appointments, Appointment::getServiceTypeId))));
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

    private String normalizeRiskChange(String riskChange) {
        return riskChange == null || riskChange.isBlank() ? null : riskChange.trim().toUpperCase();
    }

    private record DateRange(LocalDateTime fromAt, LocalDateTime toAt) {
    }

    private record RelatedData(
            Map<Long, Student> students,
            Map<Long, Campus> campuses,
            Map<Long, Room> rooms,
            Map<Long, ServiceType> serviceTypes) {
    }
}
