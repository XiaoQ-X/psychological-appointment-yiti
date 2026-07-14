package cn.schoolpsych.appointment.student.auth;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.account.AccountStatus;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.StudentRepository;
import cn.schoolpsych.appointment.security.AccountLoginAttemptService;
import cn.schoolpsych.appointment.security.TokenService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

class StudentAuthServiceTest {

    @Test
    void loginRecordsPasswordFailureOutsideLoginTransaction() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        StudentRepository studentRepository = mock(StudentRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        TokenService tokenService = mock(TokenService.class);
        AccountLoginAttemptService loginAttemptService = mock(AccountLoginAttemptService.class);
        StudentAuthService service = new StudentAuthService(
                accountRepository, studentRepository, passwordEncoder, tokenService, loginAttemptService);
        Account account = activeAccount();
        when(accountRepository.findByUsername("20260001")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(new StudentLoginRequest("20260001", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class);

        verify(loginAttemptService).recordFailure(11L);
    }

    private Account activeAccount() {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(11L);
        when(account.getPasswordHash()).thenReturn("hash");
        when(account.getRole()).thenReturn(AccountRole.STUDENT);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(account.isLockedNow()).thenReturn(false);
        return account;
    }
}
