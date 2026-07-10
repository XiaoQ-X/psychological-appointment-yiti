package cn.schoolpsych.appointment.domain.appointment;

import java.time.LocalDateTime;
import java.util.List;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
public class Appointment extends BaseEntity {

    private static final List<AppointmentStatus> STUDENT_CANCELABLE_STATUSES = List.of(
            AppointmentStatus.SUBMITTED,
            AppointmentStatus.RISK_REVIEW,
            AppointmentStatus.COUNSELOR_REVIEW,
            AppointmentStatus.ADMIN_REVIEW,
            AppointmentStatus.CONFIRMED);

    private static final List<AppointmentStatus> COUNSELOR_COMPLETABLE_STATUSES = List.of(
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.CHECKED_IN);

    private static final List<AppointmentStatus> ADMIN_CANCELABLE_STATUSES = List.of(
            AppointmentStatus.SUBMITTED,
            AppointmentStatus.RISK_REVIEW,
            AppointmentStatus.COUNSELOR_REVIEW,
            AppointmentStatus.ADMIN_REVIEW,
            AppointmentStatus.CONFIRMED,
            AppointmentStatus.CHECKED_IN);

    @Column(name = "appointment_no", nullable = false, unique = true, length = 64)
    private String appointmentNo;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Column(name = "slot_id", nullable = false)
    private Long slotId;

    @Column(name = "campus_id", nullable = false)
    private Long campusId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "service_type_id", nullable = false)
    private Long serviceTypeId;

    @Column(name = "semester_id", nullable = false)
    private Long semesterId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "rule_set_id", nullable = false)
    private Long ruleSetId;

    @Column(name = "consent_record_id", nullable = false)
    private Long consentRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AppointmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "canceled_by")
    private Long canceledBy;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    protected Appointment() {
    }

    public static Appointment create(
            String appointmentNo,
            Long studentId,
            AppointmentSlotSnapshot slot,
            Long semesterId,
            Long ruleSetId,
            Long consentRecordId,
            AppointmentStatus status,
            RiskLevel riskLevel) {
        Appointment appointment = new Appointment();
        appointment.appointmentNo = appointmentNo;
        appointment.studentId = studentId;
        appointment.counselorId = slot.counselorId();
        appointment.slotId = slot.slotId();
        appointment.campusId = slot.campusId();
        appointment.roomId = slot.roomId();
        appointment.serviceTypeId = slot.serviceTypeId();
        appointment.semesterId = semesterId;
        appointment.startAt = slot.startAt();
        appointment.endAt = slot.endAt();
        appointment.ruleSetId = ruleSetId;
        appointment.consentRecordId = consentRecordId;
        appointment.status = status;
        appointment.riskLevel = riskLevel;
        return appointment;
    }

    public String getAppointmentNo() {
        return appointmentNo;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getCounselorId() {
        return counselorId;
    }

    public Long getSlotId() {
        return slotId;
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

    public AppointmentStatus getStatus() {
        return status;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public boolean canBeCanceledByStudent(LocalDateTime now) {
        return STUDENT_CANCELABLE_STATUSES.contains(status) && startAt.isAfter(now);
    }

    public void cancelByStudent(Long actorAccountId, String reason) {
        this.status = AppointmentStatus.CANCELED_BY_STUDENT;
        this.cancelReason = reason;
        this.canceledBy = actorAccountId;
        this.canceledAt = LocalDateTime.now();
    }

    public boolean canBeCanceledByAdmin() {
        return ADMIN_CANCELABLE_STATUSES.contains(status);
    }

    public void cancelByAdmin(Long actorAccountId, String reason) {
        this.status = AppointmentStatus.CANCELED_BY_ADMIN;
        this.cancelReason = reason;
        this.canceledBy = actorAccountId;
        this.canceledAt = LocalDateTime.now();
    }

    public boolean canBeRiskReviewed() {
        return status == AppointmentStatus.RISK_REVIEW && riskLevel == RiskLevel.HIGH;
    }

    public void approveRiskReview() {
        this.status = AppointmentStatus.CONFIRMED;
    }

    public void referAfterRiskReview() {
        this.status = AppointmentStatus.REFERRED;
    }

    public void closeAfterRiskReview() {
        this.status = AppointmentStatus.CLOSED;
    }

    public boolean canBeCompletedByCounselor() {
        return COUNSELOR_COMPLETABLE_STATUSES.contains(status);
    }

    public void complete() {
        this.status = AppointmentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }
}
