package cn.schoolpsych.appointment.security;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountStatus;
import cn.schoolpsych.appointment.repository.AccountRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final AccountRepository accountRepository;

    public BearerTokenAuthenticationFilter(TokenService tokenService, AccountRepository accountRepository) {
        this.tokenService = tokenService;
        this.accountRepository = accountRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            authenticate(authorization.substring(7));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            TokenClaims claims = tokenService.parse(token);
            Account account = accountRepository.findById(claims.accountId()).orElse(null);
            if (account == null
                    || account.getStatus() != AccountStatus.ACTIVE
                    || !Objects.equals(claims.username(), account.getUsername())
                    || claims.role() != account.getRole()
                    || !Objects.equals(claims.passwordVersion(), account.passwordVersion())) {
                return;
            }
            AuthenticatedAccount principal = new AuthenticatedAccount(account.getId(), account.getUsername(), account.getRole());
            String authority = account.isForcePasswordChange() && requiresInitialPasswordChange(account)
                    ? "ROLE_" + account.getRole().name() + "_PASSWORD_CHANGE_REQUIRED"
                    : "ROLE_" + account.getRole().name();
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority(authority)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (IllegalArgumentException ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean requiresInitialPasswordChange(Account account) {
        return account.getRole() == cn.schoolpsych.appointment.domain.account.AccountRole.STUDENT
                || account.getRole() == cn.schoolpsych.appointment.domain.account.AccountRole.COUNSELOR;
    }
}
