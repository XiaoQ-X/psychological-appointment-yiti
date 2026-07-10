package cn.schoolpsych.appointment.counselor.auth;

import cn.schoolpsych.appointment.domain.account.AccountRole;

public record CounselorLoginResponse(
        String accessToken,
        String tokenType,
        long expiresAtEpochSeconds,
        Long accountId,
        Long counselorId,
        String username,
        String counselorName,
        AccountRole role,
        boolean forcePasswordChange) {
}
