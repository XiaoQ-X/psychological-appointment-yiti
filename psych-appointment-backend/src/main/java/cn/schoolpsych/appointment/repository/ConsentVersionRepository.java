package cn.schoolpsych.appointment.repository;

import java.util.Optional;

import cn.schoolpsych.appointment.domain.consent.ConsentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentVersionRepository extends JpaRepository<ConsentVersion, Long> {
    Optional<ConsentVersion> findFirstByStatusOrderByPublishedAtDesc(String status);
}
