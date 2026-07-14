package cn.schoolpsych.appointment.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

class BearerTokenAuthenticationFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void initialPasswordTokenHasOnlyPasswordChangeAuthority() throws Exception {
        Fixture fixture = fixture();
        authenticate(fixture.filter(), fixture.initialToken());

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_STUDENT_PASSWORD_CHANGE_REQUIRED");
    }

    @Test
    void passwordChangeInvalidatesOldTokenAndNewTokenGetsStudentAuthority() throws Exception {
        Fixture fixture = fixture();
        fixture.account().changePassword("new-hash");
        String newToken = fixture.tokenService().createAccessToken(fixture.account());

        authenticate(fixture.filter(), fixture.initialToken());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        authenticate(fixture.filter(), newToken);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_STUDENT");
    }

    private void authenticate(BearerTokenAuthenticationFilter filter, String token) throws Exception {
        SecurityContextHolder.clearContext();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    private Fixture fixture() {
        TokenService tokenService = new TokenService(
                new ObjectMapper(), "0123456789abcdef0123456789abcdef", 30);
        Account account = Account.create("20260001", "hash", AccountRole.STUDENT, true);
        setId(account, 42L);
        AccountRepository accountRepository = mock(AccountRepository.class);
        when(accountRepository.findById(42L)).thenReturn(Optional.of(account));
        String initialToken = tokenService.createAccessToken(account);
        return new Fixture(
                account,
                tokenService,
                new BearerTokenAuthenticationFilter(tokenService, accountRepository),
                initialToken);
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

    private record Fixture(
            Account account,
            TokenService tokenService,
            BearerTokenAuthenticationFilter filter,
            String initialToken) {
    }
}
