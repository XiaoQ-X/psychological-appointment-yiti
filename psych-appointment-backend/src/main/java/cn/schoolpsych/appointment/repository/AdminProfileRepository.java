package cn.schoolpsych.appointment.repository;

import cn.schoolpsych.appointment.domain.admin.AdminProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminProfileRepository extends JpaRepository<AdminProfile, Long> {
}
