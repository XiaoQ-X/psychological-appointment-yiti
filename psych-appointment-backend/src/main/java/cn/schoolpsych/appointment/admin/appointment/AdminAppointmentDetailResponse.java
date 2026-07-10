package cn.schoolpsych.appointment.admin.appointment;

import java.time.LocalDateTime;
import java.util.List;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.domain.appointment.RiskReviewStatus;
import cn.schoolpsych.appointment.domain.referral.ReferralStatus;
import cn.schoolpsych.appointment.domain.referral.ReferralType;

public record AdminAppointmentDetailResponse(
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
        boolean firstVisit,
        List<String> issueTypes,
        RiskLevel urgencyLevel,
        String contactTime,
        boolean selfHarm,
        boolean harmOthers,
        boolean crisisEvent,
        boolean psychiatricTreatment,
        boolean medication,
        boolean willingContact,
        RiskReviewStatus riskReviewStatus,
        Long riskReviewedBy,
        LocalDateTime riskReviewedAt,
        Long referralId,
        ReferralType referralType,
        String referralDestination,
        ReferralStatus referralStatus,
        String cancelReason,
        LocalDateTime canceledAt,
        LocalDateTime completedAt) {
}
