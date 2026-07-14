package cn.schoolpsych.appointment.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AccountPasswordServiceTest {

    @Test
    void changePasswordClearsInitialPasswordFlagAndRotatesPasswordVersion() {
        AccountRepository accountRepository = mock(AccountRepository.class);
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);
        TokenService tokenService = new TokenService(
                new ObjectMapper(), "0123456789abcdef0123456789abcdef", 30);
        Account account = Account.create(
                "20260001", passwordEncoder.encode("Initial123!"), AccountRole.STUDENT, true);
        setId(account, 42L);
        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));
        AccountPasswordService service = new AccountPasswordService(accountRepository, passwordEncoder, tokenService);
        String oldToken = tokenService.createAccessToken(account);

        ChangePasswordResponse response = service.changePassword(
                new AuthenticatedAccount(42L, "20260001", AccountRole.STUDENT),
                AccountRole.STUDENT,
                new ChangePasswordRequest("Initial123!", "Changed123!"));

        assertThat(account.isForcePasswordChange()).isFalse();
        assertThat(passwordEncoder.matches("Changed123!", account.getPasswordHash())).isTrue();
        assertThat(tokenService.parse(oldToken).passwordVersion()).isEmpty();
        assertThat(tokenService.parse(response.accessToken()).passwordVersion()).isEqualTo(account.passwordVersion());
        assertThat(account.passwordVersion()).isNotEmpty();
    }

    private void setId(Account account, Long id) {
        try {
            var field = account.getClass().getSuperclass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
