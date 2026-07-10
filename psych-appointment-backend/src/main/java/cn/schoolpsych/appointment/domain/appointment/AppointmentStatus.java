package cn.schoolpsych.appointment.domain.appointment;

public enum AppointmentStatus {
    DRAFT,
    SUBMITTED,
    RISK_REVIEW,
    COUNSELOR_REVIEW,
    ADMIN_REVIEW,
    CONFIRMED,
    CANCELED_BY_STUDENT,
    CANCELED_BY_COUNSELOR,
    CANCELED_BY_ADMIN,
    CHECKED_IN,
    NO_SHOW,
    COMPLETED,
    REFERRED,
    CLOSED
}
