package cn.schoolpsych.appointment.student.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;

public record SubmitAppointmentResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus status,
        RiskLevel riskLevel,
        Long slotId,
        Long counselorId,
        LocalDateTime startAt,
        LocalDateTime endAt) {
}
