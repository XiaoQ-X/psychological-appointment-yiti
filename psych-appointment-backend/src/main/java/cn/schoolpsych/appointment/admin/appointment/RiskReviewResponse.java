package cn.schoolpsych.appointment.admin.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskReviewStatus;
import cn.schoolpsych.appointment.domain.referral.ReferralStatus;
import cn.schoolpsych.appointment.domain.referral.ReferralType;
import cn.schoolpsych.appointment.domain.schedule.SlotStatus;

public record RiskReviewResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus appointmentStatus,
        RiskReviewStatus riskReviewStatus,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        Long referralId,
        ReferralType referralType,
        String referralDestination,
        ReferralStatus referralStatus,
        Long slotId,
        SlotStatus slotStatus) {
}
