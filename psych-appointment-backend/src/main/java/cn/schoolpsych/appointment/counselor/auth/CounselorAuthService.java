package cn.schoolpsych.appointment.counselor.auth;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.account.AccountStatus;
import cn.schoolpsych.appointment.domain.counselor.Counselor;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.CounselorRepository;
import cn.schoolpsych.appointment.security.TokenClaims;
import cn.schoolpsych.appointment.security.TokenService;
import cn.schoolpsych.appointment.security.AccountLoginAttemptService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CounselorAuthService {

    private final AccountRepository accountRepository;
    private final CounselorRepository counselorRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AccountLoginAttemptService loginAttemptService;

    public CounselorAuthService(
            AccountRepository accountRepository,
            CounselorRepository counselorRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            AccountLoginAttemptService loginAttemptService) {
        this.accountRepository = accountRepository;
        this.counselorRepository = counselorRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public CounselorLoginResponse login(CounselorLoginRequest request) {
        Account account = accountRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        account.unlockIfExpired();
        if (account.getStatus() != AccountStatus.ACTIVE || account.isLockedNow() || account.getRole() != AccountRole.COUNSELOR) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            loginAttemptService.recordFailure(account.getId());
            throw new BadCredentialsException("Invalid username or password");
        }
        Counselor counselor = counselorRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        if (!"ACTIVE".equals(counselor.getStatus())) {
            throw new BadCredentialsException("Invalid username or password");
        }
        account.markLoginSuccess();
        String token = tokenService.createAccessToken(account);
        TokenClaims claims = tokenService.parse(token);
        return new CounselorLoginResponse(
                token,
                "Bearer",
                claims.expiresAtEpochSeconds(),
                account.getId(),
                counselor.getId(),
                account.getUsername(),
                counselor.getName(),
                account.getRole(),
                account.isForcePasswordChange());
    }
}
