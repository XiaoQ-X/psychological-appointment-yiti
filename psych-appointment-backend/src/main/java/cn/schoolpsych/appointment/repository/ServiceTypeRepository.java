package cn.schoolpsych.appointment.repository;

import java.util.List;

import cn.schoolpsych.appointment.domain.service.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, Long> {
    List<ServiceType> findByEnabledTrueOrderByIdAsc();
}
