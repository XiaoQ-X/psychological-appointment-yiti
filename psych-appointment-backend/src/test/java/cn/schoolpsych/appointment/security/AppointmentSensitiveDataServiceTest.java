package cn.schoolpsych.appointment.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
import cn.schoolpsych.appointment.domain.appointment.AppointmentFormMetadata;
import cn.schoolpsych.appointment.domain.appointment.AppointmentSlotSnapshot;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskAssessment;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.domain.appointment.RiskReviewMetadata;
import cn.schoolpsych.appointment.domain.appointment.RiskScreeningAnswers;
import cn.schoolpsych.appointment.domain.referral.Referral;
import cn.schoolpsych.appointment.domain.referral.ReferralType;
import org.junit.jupiter.api.Test;

class AppointmentSensitiveDataServiceTest {

    private final AppointmentSensitiveDataService sensitiveData = sensitiveData();

    @Test
    void encryptedAppointmentValuesRoundTripWithoutPlaintextStorage() {
        String issueMarker = "synthetic-private-category";
        String contactMarker = "synthetic-contact-window";
        byte[] formMetadataEncrypted = sensitiveData.encryptFormMetadata(
                List.of(issueMarker), RiskLevel.HIGH, contactMarker);
        AppointmentForm form = AppointmentForm.create(
                1L, true, formMetadataEncrypted, new byte[]{1}, null);

        RiskScreeningAnswers answers = new RiskScreeningAnswers(true, false, true, true, false, true);
        RiskAssessment risk = RiskAssessment.create(
                1L, sensitiveData.encryptRiskAnswers(answers), RiskLevel.HIGH);
        LocalDateTime reviewedAt = LocalDateTime.now().withNano(123_456_000);
        risk.review(
                cn.schoolpsych.appointment.domain.appointment.RiskReviewStatus.REFERRED,
                new byte[]{2},
                sensitiveData.encryptReviewMetadata(99L, reviewedAt));

        Appointment appointment = appointment();
        String cancellationMarker = "synthetic-cancellation-reason";
        appointment.cancelByAdmin(99L, sensitiveData.encryptText(cancellationMarker));
        String destinationMarker = "synthetic-referral-destination";
        Referral referral = Referral.open(
                1L, 2L, 3L, ReferralType.HOSPITAL,
                sensitiveData.encryptText(destinationMarker), new byte[]{3});

        assertCiphertextHides(formMetadataEncrypted, issueMarker, contactMarker);
        assertCiphertextHides(risk.getAnswersEncrypted(), "selfHarm");
        assertCiphertextHides(appointment.getCancelReasonEncrypted(), cancellationMarker);
        assertCiphertextHides(referral.getDestinationEncrypted(), destinationMarker);

        AppointmentFormMetadata decodedForm = sensitiveData.readFormMetadata(form);
        assertThat(decodedForm.issueTypes()).containsExactly(issueMarker);
        assertThat(decodedForm.urgencyLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(decodedForm.contactTime()).isEqualTo(contactMarker);
        assertThat(sensitiveData.readRiskAnswers(risk)).isEqualTo(answers);
        assertThat(sensitiveData.readReviewMetadata(risk)).isEqualTo(new RiskReviewMetadata(99L, reviewedAt));
        assertThat(sensitiveData.readCancellationReason(appointment)).isEqualTo(cancellationMarker);
        assertThat(sensitiveData.readReferralDestination(referral)).isEqualTo(destinationMarker);
    }

    private Appointment appointment() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        return Appointment.create(
                "APT-sensitive-test",
                2L,
                new AppointmentSlotSnapshot(3L, 4L, 5L, 6L, 7L, start, start.plusMinutes(50)),
                8L,
                9L,
                10L,
                AppointmentStatus.CONFIRMED,
                RiskLevel.HIGH);
    }

    private AppointmentSensitiveDataService sensitiveData() {
        byte[] key = new byte[32];
        for (int index = 0; index < key.length; index++) {
            key[index] = (byte) (index + 1);
        }
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SensitiveDataEncryptor encryptor = new SensitiveDataEncryptor(
                Base64.getEncoder().encodeToString(key));
        return new AppointmentSensitiveDataService(
                new SensitiveDataCodec(encryptor, objectMapper), objectMapper);
    }

    private void assertCiphertextHides(byte[] ciphertext, String... markers) {
        String encoded = Base64.getEncoder().encodeToString(ciphertext);
        for (String marker : markers) {
            assertThat(encoded).doesNotContain(marker);
        }
    }
}
