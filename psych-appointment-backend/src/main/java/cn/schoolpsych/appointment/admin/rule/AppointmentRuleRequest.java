package cn.schoolpsych.appointment.admin.rule;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AppointmentRuleRequest(
        @NotBlank @Size(max = 128) String name,
        @NotNull @Valid AppointmentRuleSettings settings) {
}
