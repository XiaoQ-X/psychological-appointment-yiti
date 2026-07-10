package cn.schoolpsych.appointment.repository;

import cn.schoolpsych.appointment.domain.student.StudentImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentImportBatchRepository extends JpaRepository<StudentImportBatch, Long> {
}
