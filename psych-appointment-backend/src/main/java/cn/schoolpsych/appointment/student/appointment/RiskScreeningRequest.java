package cn.schoolpsych.appointment.student.appointment;

public record RiskScreeningRequest(
        boolean selfHarm,
        boolean harmOthers,
        boolean crisisEvent,
        boolean psychiatricTreatment,
        boolean medication,
        boolean willingContact) {
}
