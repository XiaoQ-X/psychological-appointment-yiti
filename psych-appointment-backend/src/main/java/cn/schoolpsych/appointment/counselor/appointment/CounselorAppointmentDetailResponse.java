package cn.schoolpsych.appointment.counselor.appointment;

import java.time.LocalDateTime;
import java.util.List;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;

public record CounselorAppointmentDetailResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus status,
        RiskLevel riskLevel,
        Long studentId,
        String studentNo,
        String studentName,
        String gender,
        String college,
        String major,
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
        boolean firstVisit,
        List<String> issueTypes,
        RiskLevel urgencyLevel,
        String contactTime,
        String cancelReason,
        LocalDateTime canceledAt) {
}
