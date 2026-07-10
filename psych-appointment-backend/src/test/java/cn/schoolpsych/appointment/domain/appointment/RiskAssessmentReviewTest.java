package cn.schoolpsych.appointment.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RiskAssessmentReviewTest {

    @Test
    void reviewStoresReviewerStatusAndTime() {
        RiskAssessment assessment = RiskAssessment.create(
                1L,
                true,
                false,
                true,
                false,
                false,
                true,
                RiskLevel.HIGH);

        assessment.review(RiskReviewStatus.REFERRED, 99L, new byte[]{1, 2, 3});

        assertThat(assessment.getReviewStatus()).isEqualTo(RiskReviewStatus.REFERRED);
        assertThat(assessment.getReviewedBy()).isEqualTo(99L);
        assertThat(assessment.getReviewedAt()).isNotNull();
    }
}
