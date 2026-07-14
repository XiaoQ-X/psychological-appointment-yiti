package cn.schoolpsych.appointment.repository;

import java.util.Collection;
import java.util.Optional;

import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUsername(String username);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select account from Account account where account.id = :accountId")
    Optional<Account> findByIdForUpdate(@Param("accountId") Long accountId);

    boolean existsByUsername(String username);

    long countByRoleIn(Collection<AccountRole> roles);
}
