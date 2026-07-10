package cn.schoolpsych.appointment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import cn.schoolpsych.appointment.domain.appointment.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, Long> {
    Optional<RiskAssessment> findByAppointmentId(Long appointmentId);

    List<RiskAssessment> findByAppointmentIdIn(Collection<Long> appointmentIds);
}
