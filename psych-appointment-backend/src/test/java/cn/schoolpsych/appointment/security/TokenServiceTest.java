package cn.schoolpsych.appointment.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    @Test
    void createsAndParsesAccessToken() {
        TokenService tokenService = new TokenService(
                new ObjectMapper(),
                "0123456789abcdef0123456789abcdef",
                30);
        Account account = Account.create("admin", "hash", AccountRole.SUPER_ADMIN, true);
        setId(account, 42L);

        String token = tokenService.createAccessToken(account);
        TokenClaims claims = tokenService.parse(token);

        assertThat(claims.accountId()).isEqualTo(42L);
        assertThat(claims.username()).isEqualTo("admin");
        assertThat(claims.role()).isEqualTo(AccountRole.SUPER_ADMIN);
        assertThat(claims.expiresAtEpochSeconds()).isPositive();
        assertThat(claims.passwordVersion()).isEmpty();
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
