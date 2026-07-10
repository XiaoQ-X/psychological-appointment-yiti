package cn.schoolpsych.appointment.admin.management;

import java.time.LocalDate;

public record GenerateSlotsResponse(
        LocalDate startDate,
        LocalDate endDate,
        Long counselorId,
        int generatedCount,
        int existingCount,
        int skippedPastCount,
        int skippedDisabledServiceCount,
        int skippedLimitCount) {
}
