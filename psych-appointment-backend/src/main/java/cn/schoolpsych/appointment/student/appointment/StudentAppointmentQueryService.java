package cn.schoolpsych.appointment.student.appointment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
import cn.schoolpsych.appointment.domain.appointment.AppointmentFormMetadata;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.common.BaseEntity;
import cn.schoolpsych.appointment.domain.counselor.Counselor;
import cn.schoolpsych.appointment.domain.location.Campus;
import cn.schoolpsych.appointment.domain.location.Room;
import cn.schoolpsych.appointment.domain.service.ServiceType;
import cn.schoolpsych.appointment.domain.student.Student;
import cn.schoolpsych.appointment.repository.AppointmentFormRepository;
import cn.schoolpsych.appointment.repository.AppointmentRepository;
import cn.schoolpsych.appointment.repository.CampusRepository;
import cn.schoolpsych.appointment.repository.CounselorRepository;
import cn.schoolpsych.appointment.repository.RoomRepository;
import cn.schoolpsych.appointment.repository.ServiceTypeRepository;
import cn.schoolpsych.appointment.repository.StudentRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import cn.schoolpsych.appointment.security.AppointmentSensitiveDataService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAppointmentQueryService {

    private final StudentRepository studentRepository;
    private final AppointmentRepository appointmentRepository;
    private final AppointmentFormRepository formRepository;
    private final CounselorRepository counselorRepository;
    private final CampusRepository campusRepository;
    private final RoomRepository roomRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final AppointmentSensitiveDataService sensitiveData;
    private final StudentCancellationPolicy cancellationPolicy;

    public StudentAppointmentQueryService(
            StudentRepository studentRepository,
            AppointmentRepository appointmentRepository,
            AppointmentFormRepository formRepository,
            CounselorRepository counselorRepository,
            CampusRepository campusRepository,
            RoomRepository roomRepository,
            ServiceTypeRepository serviceTypeRepository,
            AppointmentSensitiveDataService sensitiveData,
            StudentCancellationPolicy cancellationPolicy) {
        this.studentRepository = studentRepository;
        this.appointmentRepository = appointmentRepository;
        this.formRepository = formRepository;
        this.counselorRepository = counselorRepository;
        this.campusRepository = campusRepository;
        this.roomRepository = roomRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.sensitiveData = sensitiveData;
        this.cancellationPolicy = cancellationPolicy;
    }

    @Transactional(readOnly = true)
    public List<StudentAppointmentRecordResponse> list(
            AuthenticatedAccount principal,
            AppointmentStatus status,
            LocalDate from,
            LocalDate to) {
        Student student = studentRepository.findByAccountId(principal.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Student account not found"));
        DateRange range = toDateRange(from, to);
        List<Appointment> appointments = appointmentRepository.findStudentAppointments(
                student.getId(),
                status,
                range.fromAt(),
                range.toAt());
        RelatedData relatedData = relatedData(appointments);
        return appointments.stream()
                .map(appointment -> toRecord(appointment, relatedData))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentAppointmentDetailResponse detail(Long appointmentId, AuthenticatedAccount principal) {
        Student student = studentRepository.findByAccountId(principal.accountId())
                .orElseThrow(() -> new IllegalArgumentException("Student account not found"));
        Appointment appointment = appointmentRepository.findByIdAndStudentId(appointmentId, student.getId())
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        RelatedData relatedData = relatedData(List.of(appointment));
        AppointmentForm form = formRepository.findByAppointmentId(appointment.getId()).orElse(null);
        Counselor counselor = relatedData.counselors().get(appointment.getCounselorId());
        Campus campus = relatedData.campuses().get(appointment.getCampusId());
        Room room = appointment.getRoomId() == null ? null : relatedData.rooms().get(appointment.getRoomId());
        ServiceType serviceType = relatedData.serviceTypes().get(appointment.getServiceTypeId());
        StudentCancellationPolicy.CancellationAvailability cancellation =
                cancellationPolicy.evaluate(appointment, LocalDateTime.now());
        AppointmentFormMetadata formMetadata = sensitiveData.readFormMetadata(form);
        return new StudentAppointmentDetailResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                appointment.getRiskLevel(),
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
                sensitiveData.readCancellationReason(appointment),
                appointment.getCanceledAt(),
                cancellation.canCancel(),
                cancellation.minCancelHoursAhead(),
                cancellation.cancelDeadline());
    }

    private StudentAppointmentRecordResponse toRecord(Appointment appointment, RelatedData relatedData) {
        Counselor counselor = relatedData.counselors().get(appointment.getCounselorId());
        Campus campus = relatedData.campuses().get(appointment.getCampusId());
        Room room = appointment.getRoomId() == null ? null : relatedData.rooms().get(appointment.getRoomId());
        ServiceType serviceType = relatedData.serviceTypes().get(appointment.getServiceTypeId());
        return new StudentAppointmentRecordResponse(
                appointment.getId(),
                appointment.getAppointmentNo(),
                appointment.getStatus(),
                appointment.getRiskLevel(),
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
                sensitiveData.readCancellationReason(appointment),
                appointment.getCanceledAt());
    }

    private RelatedData relatedData(List<Appointment> appointments) {
        return new RelatedData(
                mapById(counselorRepository.findAllById(ids(appointments, Appointment::getCounselorId))),
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

    private record DateRange(LocalDateTime fromAt, LocalDateTime toAt) {
    }

    private record RelatedData(
            Map<Long, Counselor> counselors,
            Map<Long, Campus> campuses,
            Map<Long, Room> rooms,
            Map<Long, ServiceType> serviceTypes) {
    }
}
