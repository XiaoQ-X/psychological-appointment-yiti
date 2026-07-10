package cn.schoolpsych.appointment.student.auth;

import cn.schoolpsych.appointment.domain.account.AccountRole;

public record StudentLoginResponse(
        String accessToken,
        String tokenType,
        long expiresAtEpochSeconds,
        Long accountId,
        Long studentId,
        String username,
        AccountRole role,
        boolean forcePasswordChange) {
}
