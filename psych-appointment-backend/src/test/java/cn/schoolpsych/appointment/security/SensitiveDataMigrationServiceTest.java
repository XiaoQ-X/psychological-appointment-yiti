package cn.schoolpsych.appointment.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
import cn.schoolpsych.appointment.domain.appointment.AppointmentSlotSnapshot;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskAssessment;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.domain.referral.Referral;
import cn.schoolpsych.appointment.domain.referral.ReferralType;
import cn.schoolpsych.appointment.repository.AppointmentFormRepository;
import cn.schoolpsych.appointment.repository.AppointmentRepository;
import cn.schoolpsych.appointment.repository.ReferralRepository;
import cn.schoolpsych.appointment.repository.RiskAssessmentRepository;
import org.junit.jupiter.api.Test;

class SensitiveDataMigrationServiceTest {

    @Test
    void migratesLegacyValuesAndClearsPlaintextFields() {
        AppointmentSensitiveDataService sensitiveData = sensitiveData();
        Appointment appointment = appointment();
        set(appointment, "cancelReason", "legacy-cancel-marker");

        AppointmentForm form = AppointmentForm.create(1L, true, new byte[]{1}, new byte[]{2}, null);
        set(form, "metadataEncrypted", null);
        set(form, "issueTypesJson", "[\"emotion\"]");
        set(form, "urgencyLevel", RiskLevel.HIGH);
        set(form, "contactTime", "legacy-contact-marker");

        RiskAssessment risk = RiskAssessment.create(1L, new byte[]{1}, RiskLevel.HIGH);
        set(risk, "answersEncrypted", null);
        set(risk, "selfHarm", true);
        set(risk, "crisisEvent", true);
        set(risk, "willingContact", true);
        set(risk, "reviewedBy", 99L);
        LocalDateTime reviewedAt = LocalDateTime.now().withNano(123_456_000);
        set(risk, "reviewedAt", reviewedAt);

        Referral referral = Referral.open(
                1L, 2L, 3L, ReferralType.HOSPITAL, new byte[]{1}, new byte[]{2});
        set(referral, "destinationEncrypted", null);
        set(referral, "destination", "legacy-destination-marker");

        AppointmentRepository appointments = mock(AppointmentRepository.class);
        AppointmentFormRepository forms = mock(AppointmentFormRepository.class);
        RiskAssessmentRepository risks = mock(RiskAssessmentRepository.class);
        ReferralRepository referrals = mock(ReferralRepository.class);
        when(appointments.findAll()).thenReturn(List.of(appointment));
        when(forms.findAll()).thenReturn(List.of(form));
        when(risks.findAll()).thenReturn(List.of(risk));
        when(referrals.findAll()).thenReturn(List.of(referral));
        SensitiveDataMigrationService migration = new SensitiveDataMigrationService(
                appointments, forms, risks, referrals, sensitiveData);

        SensitiveDataMigrationService.MigrationResult result = migration.migrateLegacyPlaintext();

        assertThat(result.total()).isEqualTo(4);
        assertThat(appointment.getLegacyCancelReason()).isNull();
        assertThat(form.getLegacyIssueTypesJson()).isNull();
        assertThat(form.getLegacyUrgencyLevel()).isNull();
        assertThat(form.getLegacyContactTime()).isNull();
        assertThat(risk.isSelfHarm()).isFalse();
        assertThat(risk.isCrisisEvent()).isFalse();
        assertThat(risk.getReviewedBy()).isNull();
        assertThat(risk.getReviewedAt()).isNull();
        assertThat(referral.getLegacyDestination()).isNull();

        assertThat(sensitiveData.readCancellationReason(appointment)).isEqualTo("legacy-cancel-marker");
        assertThat(sensitiveData.readFormMetadata(form).issueTypes()).containsExactly("emotion");
        assertThat(sensitiveData.readFormMetadata(form).contactTime()).isEqualTo("legacy-contact-marker");
        assertThat(sensitiveData.readRiskAnswers(risk).selfHarm()).isTrue();
        assertThat(sensitiveData.readRiskAnswers(risk).crisisEvent()).isTrue();
        assertThat(sensitiveData.readReviewMetadata(risk).reviewedBy()).isEqualTo(99L);
        assertThat(sensitiveData.readReviewMetadata(risk).reviewedAt()).isEqualTo(reviewedAt);
        assertThat(sensitiveData.readReferralDestination(referral)).isEqualTo("legacy-destination-marker");

        assertThat(migration.migrateLegacyPlaintext().total()).isZero();
    }

    private Appointment appointment() {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        return Appointment.create(
                "APT-migration-test",
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
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        SensitiveDataEncryptor encryptor = new SensitiveDataEncryptor(
                Base64.getEncoder().encodeToString(key));
        return new AppointmentSensitiveDataService(
                new SensitiveDataCodec(encryptor, objectMapper), objectMapper);
    }

    private void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError(exception);
        }
    }
}
