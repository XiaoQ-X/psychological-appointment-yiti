package cn.schoolpsych.appointment.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AppointmentAdminWorkflowTest {

    @Test
    void adminCanCancelActiveAppointment() {
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED, RiskLevel.LOW);

        assertThat(appointment.canBeCanceledByAdmin()).isTrue();

        byte[] encryptedReason = new byte[]{1, 2, 3};
        appointment.cancelByAdmin(99L, encryptedReason);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELED_BY_ADMIN);
        assertThat(appointment.getLegacyCancelReason()).isNull();
        assertThat(appointment.getCancelReasonEncrypted()).isEqualTo(encryptedReason);
        assertThat(appointment.getCanceledAt()).isNotNull();
    }

    @Test
    void highRiskReviewCanApproveReferOrClose() {
        Appointment approve = appointment(AppointmentStatus.RISK_REVIEW, RiskLevel.HIGH);
        Appointment refer = appointment(AppointmentStatus.RISK_REVIEW, RiskLevel.HIGH);
        Appointment close = appointment(AppointmentStatus.RISK_REVIEW, RiskLevel.HIGH);

        assertThat(approve.canBeRiskReviewed()).isTrue();

        approve.approveRiskReview();
        refer.referAfterRiskReview();
        close.closeAfterRiskReview();

        assertThat(approve.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(refer.getStatus()).isEqualTo(AppointmentStatus.REFERRED);
        assertThat(close.getStatus()).isEqualTo(AppointmentStatus.CLOSED);
    }

    private Appointment appointment(AppointmentStatus status, RiskLevel riskLevel) {
        LocalDateTime startAt = LocalDateTime.now().plusDays(1);
        return Appointment.create(
                "APT-admin-test",
                1L,
                new AppointmentSlotSnapshot(2L, 3L, 4L, 5L, 6L, startAt, startAt.plusMinutes(50)),
                7L,
                8L,
                9L,
                status,
                riskLevel);
    }
}
