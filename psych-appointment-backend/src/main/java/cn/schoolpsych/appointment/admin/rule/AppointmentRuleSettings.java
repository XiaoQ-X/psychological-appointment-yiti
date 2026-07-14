package cn.schoolpsych.appointment.admin.rule;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AppointmentRuleSettings(
        @Min(0) @Max(120) int slotGapMinutes,
        @Min(1) @Max(60) int slotLockMinutes,
        @Min(1) @Max(60) int maxBookingDaysAhead,
        @Min(0) @Max(336) int minBookingHoursAhead,
        @Min(0) @Max(336) int minCancelHoursAhead,
        @Min(1) @Max(10) int maxWeeklyAppointments,
        @Min(1) @Max(100) int maxSemesterCompletedAppointments,
        @Min(1) @Max(5) int maxActiveAppointments,
        @Min(1) @Max(10) Integer noShowRestrictThreshold) {

    public static AppointmentRuleSettings defaults() {
        return new AppointmentRuleSettings(10, 10, 14, 24, 24, 1, 8, 1, 2);
    }
}
