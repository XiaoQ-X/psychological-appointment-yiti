package cn.schoolpsych.appointment.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    long countByStudentIdAndStatusIn(Long studentId, Collection<AppointmentStatus> statuses);

    long countByStudentIdAndStatusInAndStartAtGreaterThanEqualAndStartAtLessThan(
            Long studentId,
            Collection<AppointmentStatus> statuses,
            LocalDateTime startAt,
            LocalDateTime endAt);

    long countByStudentIdAndSemesterIdAndStatus(
            Long studentId,
            Long semesterId,
            AppointmentStatus status);

    @Query("""
            select appointment from Appointment appointment
            where appointment.studentId = :studentId
              and (:status is null or appointment.status = :status)
              and (:fromAt is null or appointment.startAt >= :fromAt)
              and (:toAt is null or appointment.startAt < :toAt)
            order by appointment.startAt desc
            """)
    List<Appointment> findStudentAppointments(
            @Param("studentId") Long studentId,
            @Param("status") AppointmentStatus status,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt);

    Optional<Appointment> findByIdAndStudentId(Long id, Long studentId);

    @Query("""
            select appointment from Appointment appointment
            where appointment.counselorId = :counselorId
              and (:status is null or appointment.status = :status)
              and (:fromAt is null or appointment.startAt >= :fromAt)
              and (:toAt is null or appointment.startAt < :toAt)
            order by appointment.startAt desc
            """)
    List<Appointment> findCounselorAppointments(
            @Param("counselorId") Long counselorId,
            @Param("status") AppointmentStatus status,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt);

    Optional<Appointment> findByIdAndCounselorId(Long id, Long counselorId);

    @Query("""
            select appointment from Appointment appointment
            where (:status is null or appointment.status = :status)
              and (:riskLevel is null or appointment.riskLevel = :riskLevel)
              and (:studentId is null or appointment.studentId = :studentId)
              and (:counselorId is null or appointment.counselorId = :counselorId)
              and (:fromAt is null or appointment.startAt >= :fromAt)
              and (:toAt is null or appointment.startAt < :toAt)
            order by appointment.startAt desc
            """)
    List<Appointment> findAdminAppointments(
            @Param("status") AppointmentStatus status,
            @Param("riskLevel") RiskLevel riskLevel,
            @Param("studentId") Long studentId,
            @Param("counselorId") Long counselorId,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment where appointment.id = :id and appointment.studentId = :studentId")
    Optional<Appointment> findByIdAndStudentIdForUpdate(@Param("id") Long id, @Param("studentId") Long studentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment where appointment.id = :id and appointment.counselorId = :counselorId")
    Optional<Appointment> findByIdAndCounselorIdForUpdate(@Param("id") Long id, @Param("counselorId") Long counselorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select appointment from Appointment appointment where appointment.id = :id")
    Optional<Appointment> findByIdForUpdate(@Param("id") Long id);
}
