package cn.schoolpsych.appointment.counselor.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.account.AccountStatus;
import cn.schoolpsych.appointment.domain.counselor.Counselor;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.CounselorRepository;
import cn.schoolpsych.appointment.security.TokenClaims;
import cn.schoolpsych.appointment.security.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

class CounselorAuthServiceTest {

    private AccountRepository accountRepository;
    private CounselorRepository counselorRepository;
    private PasswordEncoder passwordEncoder;
    private TokenService tokenService;
    private CounselorAuthService service;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        counselorRepository = mock(CounselorRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        tokenService = mock(TokenService.class);
        service = new CounselorAuthService(accountRepository, counselorRepository, passwordEncoder, tokenService);
    }

    @Test
    void loginReturnsCounselorIdentity() {
        Account account = account(AccountRole.COUNSELOR);
        Counselor counselor = mock(Counselor.class);
        when(counselor.getId()).thenReturn(7L);
        when(counselor.getName()).thenReturn("Counselor One");
        when(counselor.getStatus()).thenReturn("ACTIVE");
        when(accountRepository.findByUsername("counselor_001")).thenReturn(Optional.of(account));
        when(counselorRepository.findByAccountId(3L)).thenReturn(Optional.of(counselor));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);
        when(tokenService.createAccessToken(account)).thenReturn("token");
        when(tokenService.parse("token")).thenReturn(new TokenClaims(3L, "counselor_001", AccountRole.COUNSELOR, 123456789L));

        CounselorLoginResponse response = service.login(new CounselorLoginRequest("counselor_001", "Password123!"));

        assertThat(response.accessToken()).isEqualTo("token");
        assertThat(response.accountId()).isEqualTo(3L);
        assertThat(response.counselorId()).isEqualTo(7L);
        assertThat(response.role()).isEqualTo(AccountRole.COUNSELOR);
    }

    @Test
    void loginRejectsNonCounselorAccount() {
        Account studentAccount = account(AccountRole.STUDENT);
        when(accountRepository.findByUsername("student_001")).thenReturn(Optional.of(studentAccount));

        assertThatThrownBy(() -> service.login(new CounselorLoginRequest("student_001", "Password123!")))
                .isInstanceOf(BadCredentialsException.class);
    }

    private Account account(AccountRole role) {
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(3L);
        when(account.getUsername()).thenReturn(role == AccountRole.COUNSELOR ? "counselor_001" : "student_001");
        when(account.getPasswordHash()).thenReturn("hash");
        when(account.getRole()).thenReturn(role);
        when(account.getStatus()).thenReturn(AccountStatus.ACTIVE);
        when(account.isLockedNow()).thenReturn(false);
        when(account.isForcePasswordChange()).thenReturn(true);
        return account;
    }
}
