package cn.schoolpsych.appointment.repository;

import java.util.Optional;

import cn.schoolpsych.appointment.domain.note.ConsultationNote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationNoteRepository extends JpaRepository<ConsultationNote, Long> {
    boolean existsByAppointmentId(Long appointmentId);

    Optional<ConsultationNote> findByAppointmentId(Long appointmentId);
}
