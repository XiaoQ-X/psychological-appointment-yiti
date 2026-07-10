package cn.schoolpsych.appointment.counselor.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
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
    private final ObjectMapper objectMapper;

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
            ObjectMapper objectMapper) {
        this.counselorRepository = counselorRepository;
        this.appointmentRepository = appointmentRepository;
        this.formRepository = formRepository;
        this.consultationNoteRepository = consultationNoteRepository;
        this.studentRepository = studentRepository;
        this.campusRepository = campusRepository;
        this.roomRepository = roomRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.sensitiveDataEncryptor = sensitiveDataEncryptor;
        this.objectMapper = objectMapper;
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
                form == null ? List.of() : readJsonList(form.getIssueTypesJson()),
                form == null ? null : form.getUrgencyLevel(),
                form == null ? null : form.getContactTime(),
                appointment.getCancelReason(),
                appointment.getCanceledAt());
    }

    @Transactional
    public CompleteAppointmentResponse complete(
            Long appointmentId,
            CompleteAppointmentRequest request,
            AuthenticatedAccount principal) {
        Counselor counselor = counselorRepository.findByAccountId(principal.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Counselor account not found"));
        Appointment appointment = appointmentRepository.findByIdAndCounselorIdForUpdate(appointmentId, counselor.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        if (!appointment.canBeCompletedByCounselor()) {
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
        appointment.complete();
        appointmentRepository.save(appointment);
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
                appointment.getCancelReason(),
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

    private List<String> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
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
