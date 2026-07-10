package cn.schoolpsych.appointment.admin.bootstrap;

import java.util.List;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.admin.AdminProfile;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.AdminProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableConfigurationProperties(AdminBootstrapProperties.class)
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final AdminBootstrapProperties properties;
    private final AccountRepository accountRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(
            AdminBootstrapProperties properties,
            AccountRepository accountRepository,
            AdminProfileRepository adminProfileRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.accountRepository = accountRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.enabled()) {
            return;
        }
        long adminCount = accountRepository.countByRoleIn(List.of(AccountRole.ADMIN, AccountRole.SUPER_ADMIN));
        if (adminCount > 0) {
            return;
        }
        if (isBlank(properties.username()) || isBlank(properties.password())) {
            log.warn("No admin account exists. Set BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD to bootstrap the first administrator.");
            return;
        }
        if (accountRepository.existsByUsername(properties.username())) {
            log.warn("Bootstrap admin username already exists but no admin role account was found: {}", properties.username());
            return;
        }
        Account account = Account.create(
                properties.username(),
                passwordEncoder.encode(properties.password()),
                AccountRole.SUPER_ADMIN,
                true);
        accountRepository.save(account);
        adminProfileRepository.save(AdminProfile.create(account.getId(), properties.name(), properties.department()));
        log.info("Bootstrap administrator account created: {}", properties.username());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
