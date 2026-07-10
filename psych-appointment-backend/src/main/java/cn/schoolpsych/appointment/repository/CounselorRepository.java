package cn.schoolpsych.appointment.repository;

import java.util.List;
import java.util.Optional;

import cn.schoolpsych.appointment.domain.counselor.Counselor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounselorRepository extends JpaRepository<Counselor, Long> {
    Optional<Counselor> findByAccountId(Long accountId);

    List<Counselor> findByStatusOrderByIdAsc(String status);

    List<Counselor> findByStatusAndVisibleTrueOrderByIdAsc(String status);

    List<Counselor> findByCampusIdAndStatusAndVisibleTrueOrderByIdAsc(Long campusId, String status);
}
