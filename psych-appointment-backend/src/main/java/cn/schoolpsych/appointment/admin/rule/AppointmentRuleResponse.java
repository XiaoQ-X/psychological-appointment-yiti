package cn.schoolpsych.appointment.admin.rule;

import java.time.LocalDateTime;

public record AppointmentRuleResponse(
        Long id,
        String name,
        AppointmentRuleSettings settings,
        boolean active,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        Long publishedBy,
        String publishedByUsername,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer version) {
}
