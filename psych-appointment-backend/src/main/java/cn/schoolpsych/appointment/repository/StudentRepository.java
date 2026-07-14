package cn.schoolpsych.appointment.repository;

import java.util.Optional;

import cn.schoolpsych.appointment.domain.student.Student;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {
    Optional<Student> findByStudentNo(String studentNo);

    Optional<Student> findByAccountId(Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select student from Student student where student.accountId = :accountId")
    Optional<Student> findByAccountIdForUpdate(@Param("accountId") Long accountId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select student from Student student where student.id = :id")
    Optional<Student> findByIdForUpdate(@Param("id") Long id);

    boolean existsByStudentNo(String studentNo);
}
