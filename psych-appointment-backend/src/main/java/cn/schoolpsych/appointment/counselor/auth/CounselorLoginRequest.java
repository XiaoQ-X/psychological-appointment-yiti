package cn.schoolpsych.appointment.counselor.auth;

import jakarta.validation.constraints.NotBlank;

public record CounselorLoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
