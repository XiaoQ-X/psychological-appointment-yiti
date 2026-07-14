package cn.schoolpsych.appointment.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.account.AccountStatus;
import cn.schoolpsych.appointment.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({AccountLoginAttemptService.class, AccountLoginAttemptServiceTest.RollingBackLogin.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@EnableJpaAuditing
class AccountLoginAttemptServiceTest {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private RollingBackLogin rollingBackLogin;

    private Long accountId;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        Account account = accountRepository.save(Account.create(
                "locked-user", "hash", AccountRole.STUDENT, true));
        accountId = account.getId();
    }

    @Test
    void failureStateSurvivesOuterTransactionRollbackAndLocksOnFifthFailure() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertThatThrownBy(() -> rollingBackLogin.fail(accountId))
                    .isInstanceOf(BadCredentialsExceptionForTest.class);
        }

        Account persisted = accountRepository.findById(accountId).orElseThrow();
        assertThat(persisted.getLoginFailCount()).isEqualTo(5);
        assertThat(persisted.getStatus()).isEqualTo(AccountStatus.LOCKED);
        assertThat(persisted.getLockedUntil()).isNotNull();
        assertThat(persisted.isLockedNow()).isTrue();
    }

    @Service
    static class RollingBackLogin {

        private final AccountLoginAttemptService loginAttemptService;

        RollingBackLogin(AccountLoginAttemptService loginAttemptService) {
            this.loginAttemptService = loginAttemptService;
        }

        @Transactional
        public void fail(Long targetAccountId) {
            loginAttemptService.recordFailure(targetAccountId);
            throw new BadCredentialsExceptionForTest();
        }
    }

    static class BadCredentialsExceptionForTest extends RuntimeException {
    }
}
