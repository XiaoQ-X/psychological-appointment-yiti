package cn.schoolpsych.appointment.security;

import cn.schoolpsych.appointment.domain.account.AccountRole;

public record TokenClaims(Long accountId, String username, AccountRole role, long expiresAtEpochSeconds) {
}
