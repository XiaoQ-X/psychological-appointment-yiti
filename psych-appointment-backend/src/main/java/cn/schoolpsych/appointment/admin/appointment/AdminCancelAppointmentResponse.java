package cn.schoolpsych.appointment.admin.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.schedule.SlotStatus;

public record AdminCancelAppointmentResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus status,
        Long slotId,
        SlotStatus slotStatus,
        LocalDateTime canceledAt) {
}
