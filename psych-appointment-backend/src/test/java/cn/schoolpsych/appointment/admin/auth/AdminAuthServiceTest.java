package cn.schoolpsych.appointment.admin.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.account.AccountStatus;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.security.AccountLoginAttemptService;
import cn.schoolpsych.appointment.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminAuthServiceTest {

    @Test
    void loginRecordsPasswordFailureOutsideLoginTransaction() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenService tokenService = mock(TokenService.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        AccountLoginAttemptService loginAttemptService = mock(AccountLoginAttemptService.class);
        AdminAuthService service = new AdminAuthService(
                accountRepository, passwordEncoder, tokenService, auditLogService, loginAttemptService);
        Account account = activeAccount();
        when(accountRepository.findByUsername("admin")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(
                new AdminLoginRequest("admin", "wrong-password"),
                new AuditRequestMetadata("127.0.0.1", "test")))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).recordFailure(21L);
    }

    private Account activeAccount() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(21L);
        when(account.getPasswordHash()).thenReturn("hash");
        when(account.getRole()).thenReturn(AccountRole.ADMIN);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(account.isLockedNow()).thenReturn(false);
        return account;
    }
}
