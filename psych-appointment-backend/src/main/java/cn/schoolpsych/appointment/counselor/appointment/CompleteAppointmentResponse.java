package cn.schoolpsych.appointment.counselor.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.note.NoteStatus;

public record CompleteAppointmentResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus appointmentStatus,
        LocalDateTime completedAt,
        Long noteId,
        NoteStatus noteStatus,
        String riskChange,
        boolean needReferral) {
}
