package cn.schoolpsych.appointment.admin.auth;

import java.util.Map;

import cn.schoolpsych.appointment.admin.audit.AuditActions;
import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.account.AccountStatus;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.security.TokenClaims;
import cn.schoolpsych.appointment.security.TokenService;
import cn.schoolpsych.appointment.security.AccountLoginAttemptService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AuditLogService auditLogService;
    private final AccountLoginAttemptService loginAttemptService;

    public AdminAuthService(
            AccountRepository accountRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            AuditLogService auditLogService,
            AccountLoginAttemptService loginAttemptService) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.auditLogService = auditLogService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request, AuditRequestMetadata metadata) {
        Account account = accountRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        account.unlockIfExpired();
        if (account.getStatus() != AccountStatus.ACTIVE || account.isLockedNow() || !isAdmin(account.getRole())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            loginAttemptService.recordFailure(account.getId());
            throw new BadCredentialsException("Invalid username or password");
        }
        account.markLoginSuccess();
        String token = tokenService.createAccessToken(account);
        TokenClaims claims = tokenService.parse(token);
        auditLogService.record(
                account.getId(),
                AuditActions.ADMIN_LOGIN,
                "ACCOUNT",
                account.getId(),
                SensitiveLevel.NORMAL,
                metadata,
                Map.of("role", account.getRole().name()));
        return new AdminLoginResponse(
                token,
                "Bearer",
                claims.expiresAtEpochSeconds(),
                account.getId(),
                account.getUsername(),
                account.getRole(),
                account.isForcePasswordChange());
    }

    private boolean isAdmin(AccountRole role) {
        return role == AccountRole.ADMIN || role == AccountRole.SUPER_ADMIN;
    }
}
