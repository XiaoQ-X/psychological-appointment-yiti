package cn.schoolpsych.appointment.repository;

import java.util.Collection;
import java.util.Optional;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByRoleIn(Collection<AccountRole> roles);
}
