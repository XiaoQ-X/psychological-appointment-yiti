package cn.schoolpsych.appointment.admin.appointment;

import cn.schoolpsych.appointment.domain.referral.ReferralType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RiskReviewRequest(
        @NotNull RiskReviewDecision decision,
        @Size(max = 10000) String handlingNotes,
        ReferralType referralType,
        @Size(max = 255) String referralDestination,
        @Size(max = 10000) String referralReason) {
}
