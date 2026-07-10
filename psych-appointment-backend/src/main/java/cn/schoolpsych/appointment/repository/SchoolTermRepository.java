package cn.schoolpsych.appointment.repository;

import java.util.Optional;

import cn.schoolpsych.appointment.domain.rule.SchoolTerm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchoolTermRepository extends JpaRepository<SchoolTerm, Long> {
    Optional<SchoolTerm> findFirstByCurrentTrueAndStatus(String status);
}
