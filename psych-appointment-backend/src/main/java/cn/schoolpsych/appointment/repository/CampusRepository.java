package cn.schoolpsych.appointment.repository;

import java.util.List;

import cn.schoolpsych.appointment.domain.location.Campus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<Campus, Long> {
    List<Campus> findByStatusOrderByIdAsc(String status);
}
