package cn.schoolpsych.appointment.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import cn.schoolpsych.appointment.domain.schedule.AppointmentSlot;
import cn.schoolpsych.appointment.domain.schedule.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select slot from AppointmentSlot slot where slot.id = :id")
    Optional<AppointmentSlot> findByIdForUpdate(@Param("id") Long id);

    boolean existsByCounselorIdAndStartAtAndEndAt(Long counselorId, LocalDateTime startAt, LocalDateTime endAt);

    long countByCounselorIdAndStartAtGreaterThanEqualAndStartAtLessThan(Long counselorId, LocalDateTime startAt, LocalDateTime endAt);

    List<AppointmentSlot> findByCounselorIdAndStatusAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
            Long counselorId,
            SlotStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt);

    List<AppointmentSlot> findByCounselorIdInAndStatusAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
            Collection<Long> counselorIds,
            SlotStatus status,
            LocalDateTime startAt,
            LocalDateTime endAt);

    List<AppointmentSlot> findByLockedByStudentIdAndStatus(Long studentId, SlotStatus status);

    default int releaseExpiredLocks(LocalDateTime now) {
        return releaseExpiredLocks(SlotStatus.LOCKED, SlotStatus.AVAILABLE, now);
    }

    @Modifying
    @Query("""
            update AppointmentSlot slot
               set slot.status = :availableStatus,
                   slot.lockedByStudentId = null,
                   slot.lockedUntil = null
             where slot.status = :lockedStatus
               and slot.lockedUntil is not null
               and slot.lockedUntil <= :now
            """)
    int releaseExpiredLocks(
            @Param("lockedStatus") SlotStatus lockedStatus,
            @Param("availableStatus") SlotStatus availableStatus,
            @Param("now") LocalDateTime now);
}
