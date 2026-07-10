package cn.schoolpsych.appointment.student.auth;

import jakarta.validation.constraints.NotBlank;

public record StudentLoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
