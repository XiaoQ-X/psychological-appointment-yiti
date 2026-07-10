package cn.schoolpsych.appointment.repository;

import java.util.Optional;

import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppointmentFormRepository extends JpaRepository<AppointmentForm, Long> {
    Optional<AppointmentForm> findByAppointmentId(Long appointmentId);
}
