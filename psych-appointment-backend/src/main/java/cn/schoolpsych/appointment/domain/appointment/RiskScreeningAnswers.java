package cn.schoolpsych.appointment.domain.appointment;

public record RiskScreeningAnswers(
        boolean selfHarm,
        boolean harmOthers,
        boolean crisisEvent,
        boolean psychiatricTreatment,
        boolean medication,
        boolean willingContact) {
}
