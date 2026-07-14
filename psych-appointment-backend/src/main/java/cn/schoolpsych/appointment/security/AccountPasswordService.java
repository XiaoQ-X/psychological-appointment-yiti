package cn.schoolpsych.appointment.security;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.account.AccountStatus;
import cn.schoolpsych.appointment.repository.AccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountPasswordService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AccountPasswordService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public ChangePasswordResponse changePassword(
            AuthenticatedAccount principal,
            AccountRole expectedRole,
            ChangePasswordRequest request) {
        if (principal == null || principal.role() != expectedRole) {
            throw new AccessDeniedException("Account role is not allowed");
        }
        Account account = accountRepository.findById(principal.accountId())
                .orElseThrow(() -> new AccessDeniedException("Account not found"));
        if (account.getStatus() != AccountStatus.ACTIVE || account.getRole() != expectedRole) {
            throw new AccessDeniedException("Account is not active");
        }
        if (!passwordEncoder.matches(request.currentPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), account.getPasswordHash())) {
            throw new IllegalArgumentException("New password must be different from current password");
        }

        account.changePassword(passwordEncoder.encode(request.newPassword()));
        String token = tokenService.createAccessToken(account);
        TokenClaims claims = tokenService.parse(token);
        return new ChangePasswordResponse(token, "Bearer", claims.expiresAtEpochSeconds());
    }
}
