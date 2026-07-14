package cn.schoolpsych.appointment.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RiskAssessmentReviewTest {

    @Test
    void reviewStoresReviewerStatusAndTime() {
        RiskAssessment assessment = RiskAssessment.create(
                1L,
                new byte[]{4, 5, 6},
                RiskLevel.HIGH);

        byte[] reviewMetadata = new byte[]{7, 8, 9};
        assessment.review(RiskReviewStatus.REFERRED, new byte[]{1, 2, 3}, reviewMetadata);

        assertThat(assessment.getReviewStatus()).isEqualTo(RiskReviewStatus.REFERRED);
        assertThat(assessment.getReviewedBy()).isNull();
        assertThat(assessment.getReviewedAt()).isNull();
        assertThat(assessment.getReviewMetadataEncrypted()).isEqualTo(reviewMetadata);
    }
}
