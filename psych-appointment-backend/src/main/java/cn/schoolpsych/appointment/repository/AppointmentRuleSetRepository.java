package cn.schoolpsych.appointment.repository;

import java.util.List;
import java.util.Optional;

import cn.schoolpsych.appointment.domain.rule.AppointmentRuleSet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AppointmentRuleSetRepository extends JpaRepository<AppointmentRuleSet, Long> {
    Optional<AppointmentRuleSet> findFirstByActiveTrueOrderByEffectiveFromDesc();

    List<AppointmentRuleSet> findAllByOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ruleSet from AppointmentRuleSet ruleSet order by ruleSet.id")
    List<AppointmentRuleSet> findAllForUpdate();
}
