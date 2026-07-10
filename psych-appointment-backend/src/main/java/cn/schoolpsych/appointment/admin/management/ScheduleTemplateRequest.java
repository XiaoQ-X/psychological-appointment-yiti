package cn.schoolpsych.appointment.admin.management;

import java.time.LocalDate;
import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ScheduleTemplateRequest(
        @NotNull Long counselorId,
        @NotNull Long campusId,
        Long roomId,
        @NotNull Long serviceTypeId,
        @NotNull @Min(1) @Max(7) Integer dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @NotNull LocalDate effectiveFrom,
        LocalDate effectiveTo,
        @Size(max = 32) String status) {
}
