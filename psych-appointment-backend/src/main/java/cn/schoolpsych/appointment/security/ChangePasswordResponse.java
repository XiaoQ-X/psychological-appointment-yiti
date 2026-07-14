package cn.schoolpsych.appointment.security;

public record ChangePasswordResponse(
        String accessToken,
        String tokenType,
        long expiresAtEpochSeconds) {
}
