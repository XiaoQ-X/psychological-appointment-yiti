package cn.schoolpsych.appointment.repository;

import java.util.List;

import cn.schoolpsych.appointment.domain.location.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
    List<Room> findByCampusIdAndStatusOrderByIdAsc(Long campusId, String status);

    List<Room> findByStatusOrderByIdAsc(String status);
}
