package cn.schoolpsych.appointment.counselor.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;

public record CounselorAppointmentRecordResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus status,
        RiskLevel riskLevel,
        Long studentId,
        String studentNo,
        String studentName,
        String college,
        String grade,
        String className,
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
