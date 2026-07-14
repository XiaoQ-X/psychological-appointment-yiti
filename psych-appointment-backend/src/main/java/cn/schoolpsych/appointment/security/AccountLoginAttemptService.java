package cn.schoolpsych.appointment.security;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountLoginAttemptService {

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final int LOCK_MINUTES = 15;

    private final AccountRepository accountRepository;

    public AccountLoginAttemptService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long accountId) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        account.unlockIfExpired();
        account.markLoginFailed(MAX_LOGIN_FAILURES, LOCK_MINUTES);
    }
}
