package cn.schoolpsych.appointment.student.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;

public record StudentAppointmentRecordResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus status,
        RiskLevel riskLevel,
        Long counselorId,
        String counselorName,
        String counselorTitle,
        Long campusId,
        String campusName,
        Long roomId,
        String roomName,
        Long serviceTypeId,
        String serviceTypeName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        String cancelReason,
        LocalDateTime canceledAt) {
}
