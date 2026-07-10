package cn.schoolpsych.appointment.admin.management;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record GenerateSlotsRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Long counselorId) {
}
