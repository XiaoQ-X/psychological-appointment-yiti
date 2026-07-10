package cn.schoolpsych.appointment.counselor.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CompleteAppointmentRequest(
        @NotBlank @Size(max = 500) String topic,
        @NotBlank @Size(max = 10000) String summary,
        @Size(max = 32) String riskChange,
        @Size(max = 10000) String followUpPlan,
        @NotNull Boolean needReferral) {
}
