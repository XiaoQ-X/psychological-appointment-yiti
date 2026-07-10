package cn.schoolpsych.appointment.admin.auth;

import cn.schoolpsych.appointment.domain.account.AccountRole;

public record AdminLoginResponse(
        String accessToken,
        String tokenType,
        long expiresAtEpochSeconds,
        Long accountId,
        String username,
        AccountRole role,
        boolean forcePasswordChange) {
}
