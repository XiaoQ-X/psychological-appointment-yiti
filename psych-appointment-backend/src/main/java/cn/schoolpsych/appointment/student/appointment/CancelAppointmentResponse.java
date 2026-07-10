package cn.schoolpsych.appointment.student.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.schedule.SlotStatus;

public record CancelAppointmentResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus status,
        Long slotId,
        SlotStatus slotStatus,
        LocalDateTime canceledAt) {
}
