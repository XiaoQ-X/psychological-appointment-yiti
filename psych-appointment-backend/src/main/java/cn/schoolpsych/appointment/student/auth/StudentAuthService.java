package cn.schoolpsych.appointment.student.auth;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.account.AccountStatus;
import cn.schoolpsych.appointment.domain.student.Student;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.StudentRepository;
import cn.schoolpsych.appointment.security.TokenClaims;
import cn.schoolpsych.appointment.security.TokenService;
import cn.schoolpsych.appointment.security.AccountLoginAttemptService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAuthService {

    private final AccountRepository accountRepository;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final AccountLoginAttemptService loginAttemptService;

    public StudentAuthService(
            AccountRepository accountRepository,
            StudentRepository studentRepository,
            PasswordEncoder passwordEncoder,
            TokenService tokenService,
            AccountLoginAttemptService loginAttemptService) {
        this.accountRepository = accountRepository;
        this.studentRepository = studentRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public StudentLoginResponse login(StudentLoginRequest request) {
        Account account = accountRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        account.unlockIfExpired();
        if (account.getStatus() != AccountStatus.ACTIVE || account.isLockedNow() || account.getRole() != AccountRole.STUDENT) {
            throw new BadCredentialsException("Invalid username or password");
        }
        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            loginAttemptService.recordFailure(account.getId());
            throw new BadCredentialsException("Invalid username or password");
        }
        Student student = studentRepository.findByAccountId(account.getId())
                .orElseThrow(() -> new BadCredentialsException("Invalid username or password"));
        account.markLoginSuccess();
        String token = tokenService.createAccessToken(account);
        TokenClaims claims = tokenService.parse(token);
        return new StudentLoginResponse(
                token,
                "Bearer",
                claims.expiresAtEpochSeconds(),
                account.getId(),
                student.getId(),
                account.getUsername(),
                account.getRole(),
                account.isForcePasswordChange());
    }
}
