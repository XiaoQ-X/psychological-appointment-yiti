package cn.schoolpsych.appointment.domain.schedule;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointment_slots")
public class AppointmentSlot extends BaseEntity {

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Column(name = "campus_id", nullable = false)
    private Long campusId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "service_type_id", nullable = false)
    private Long serviceTypeId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SlotStatus status;

    @Column(name = "locked_by_student_id")
    private Long lockedByStudentId;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Column(name = "appointment_id")
    private Long appointmentId;

    protected AppointmentSlot() {
    }

    public static AppointmentSlot available(
            Long counselorId,
            Long campusId,
            Long roomId,
            Long serviceTypeId,
            LocalDateTime startAt,
            LocalDateTime endAt) {
        AppointmentSlot slot = new AppointmentSlot();
        slot.counselorId = counselorId;
        slot.campusId = campusId;
        slot.roomId = roomId;
        slot.serviceTypeId = serviceTypeId;
        slot.startAt = startAt;
        slot.endAt = endAt;
        slot.status = SlotStatus.AVAILABLE;
        return slot;
    }

    public boolean canBeBookedBy(Long studentId) {
        return canBeBookedBy(studentId, LocalDateTime.now());
    }

    public boolean canBeBookedBy(Long studentId, LocalDateTime now) {
        return status == SlotStatus.AVAILABLE || isLockedBy(studentId, now);
    }

    public boolean canBeLockedBy(Long studentId, LocalDateTime now) {
        return status == SlotStatus.AVAILABLE
                || isExpiredLock(now)
                || isLockedBy(studentId, now);
    }

    public boolean isLockedBy(Long studentId, LocalDateTime now) {
        return status == SlotStatus.LOCKED
                && studentId != null
                && studentId.equals(lockedByStudentId)
                && lockedUntil != null
                && lockedUntil.isAfter(now);
    }

    public boolean isExpiredLock(LocalDateTime now) {
        return status == SlotStatus.LOCKED
                && lockedUntil != null
                && !lockedUntil.isAfter(now);
    }

    public void markLocked(Long studentId, LocalDateTime lockedUntil) {
        this.status = SlotStatus.LOCKED;
        this.lockedByStudentId = studentId;
        this.lockedUntil = lockedUntil;
        this.appointmentId = null;
    }

    public void markBooked(Long appointmentId) {
        this.status = SlotStatus.BOOKED;
        this.appointmentId = appointmentId;
        this.lockedByStudentId = null;
        this.lockedUntil = null;
    }

    public void releaseLock() {
        if (this.status == SlotStatus.LOCKED) {
            this.status = SlotStatus.AVAILABLE;
            this.lockedByStudentId = null;
            this.lockedUntil = null;
        }
    }

    public void releaseBooking(Long appointmentId) {
        if (this.status == SlotStatus.BOOKED && appointmentId != null && appointmentId.equals(this.appointmentId)) {
            this.status = SlotStatus.AVAILABLE;
            this.appointmentId = null;
            this.lockedByStudentId = null;
            this.lockedUntil = null;
        }
    }

    public Long getCounselorId() {
        return counselorId;
    }

    public Long getCampusId() {
        return campusId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getServiceTypeId() {
        return serviceTypeId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public SlotStatus getStatus() {
        return status;
    }

    public Long getLockedByStudentId() {
        return lockedByStudentId;
    }

    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }
}
