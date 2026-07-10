package cn.schoolpsych.appointment.domain.referral;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ReferralTest {

    @Test
    void openCreatesOpenReferral() {
        Referral referral = Referral.open(
                1L,
                2L,
                3L,
                ReferralType.HOSPITAL,
                "City Hospital",
                new byte[]{1});

        assertThat(referral.getAppointmentId()).isEqualTo(1L);
        assertThat(referral.getStudentId()).isEqualTo(2L);
        assertThat(referral.getCounselorId()).isEqualTo(3L);
        assertThat(referral.getReferralType()).isEqualTo(ReferralType.HOSPITAL);
        assertThat(referral.getDestination()).isEqualTo("City Hospital");
        assertThat(referral.getStatus()).isEqualTo(ReferralStatus.OPEN);
    }
}
