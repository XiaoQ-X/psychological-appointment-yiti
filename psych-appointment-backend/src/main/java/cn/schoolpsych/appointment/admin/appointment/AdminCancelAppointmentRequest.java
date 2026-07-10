package cn.schoolpsych.appointment.admin.appointment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCancelAppointmentRequest(@NotBlank @Size(max = 500) String reason) {
}
