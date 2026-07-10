package cn.schoolpsych.appointment.repository;

import cn.schoolpsych.appointment.domain.consent.ConsentRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentRecordRepository extends JpaRepository<ConsentRecord, Long> {
}
