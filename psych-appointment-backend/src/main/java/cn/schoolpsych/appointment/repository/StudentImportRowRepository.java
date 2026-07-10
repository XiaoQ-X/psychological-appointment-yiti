package cn.schoolpsych.appointment.repository;

import cn.schoolpsych.appointment.domain.student.StudentImportRow;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentImportRowRepository extends JpaRepository<StudentImportRow, Long> {
}
