package cn.schoolpsych.appointment.admin.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.domain.appointment.RiskReviewStatus;

public record AdminAppointmentRecordResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus status,
        RiskLevel riskLevel,
        RiskReviewStatus riskReviewStatus,
        Long studentId,
        String studentNo,
        String studentName,
        String college,
        String grade,
        Long counselorId,
        String counselorName,
        Long campusId,
        String campusName,
        Long roomId,
        String roomName,
        Long serviceTypeId,
        String serviceTypeName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime canceledAt,
        LocalDateTime completedAt) {
}
