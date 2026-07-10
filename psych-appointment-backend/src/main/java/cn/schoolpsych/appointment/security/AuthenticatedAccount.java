package cn.schoolpsych.appointment.security;

import cn.schoolpsych.appointment.domain.account.AccountRole;

public record AuthenticatedAccount(Long accountId, String username, AccountRole role) {
}
