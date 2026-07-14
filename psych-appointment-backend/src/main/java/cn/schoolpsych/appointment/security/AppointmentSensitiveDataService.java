package cn.schoolpsych.appointment.security;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentForm;
import cn.schoolpsych.appointment.domain.appointment.AppointmentFormMetadata;
import cn.schoolpsych.appointment.domain.appointment.RiskAssessment;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.domain.appointment.RiskReviewMetadata;
import cn.schoolpsych.appointment.domain.appointment.RiskScreeningAnswers;
import cn.schoolpsych.appointment.domain.referral.Referral;
import org.springframework.stereotype.Service;

@Service
public class AppointmentSensitiveDataService {

    private final SensitiveDataCodec codec;
    private final ObjectMapper objectMapper;

    public AppointmentSensitiveDataService(SensitiveDataCodec codec, ObjectMapper objectMapper) {
        this.codec = codec;
        this.objectMapper = objectMapper;
    }

    public byte[] encryptFormMetadata(List<String> issueTypes, RiskLevel urgencyLevel, String contactTime) {
        return codec.encryptJson(new AppointmentFormMetadata(issueTypes, urgencyLevel, trimToNull(contactTime)));
    }

    public AppointmentFormMetadata readFormMetadata(AppointmentForm form) {
        if (form == null) {
            return new AppointmentFormMetadata(List.of(), null, null);
        }
        if (form.getMetadataEncrypted() != null) {
            return codec.decryptJson(form.getMetadataEncrypted(), AppointmentFormMetadata.class);
        }
        return new AppointmentFormMetadata(
                readLegacyIssueTypes(form.getLegacyIssueTypesJson()),
                form.getLegacyUrgencyLevel(),
                form.getLegacyContactTime());
    }

    public byte[] encryptRiskAnswers(RiskScreeningAnswers answers) {
        return codec.encryptJson(answers);
    }

    public RiskScreeningAnswers readRiskAnswers(RiskAssessment assessment) {
        if (assessment.getAnswersEncrypted() != null) {
            return codec.decryptJson(assessment.getAnswersEncrypted(), RiskScreeningAnswers.class);
        }
        return new RiskScreeningAnswers(
                assessment.isSelfHarm(),
                assessment.isHarmOthers(),
                assessment.isCrisisEvent(),
                assessment.isPsychiatricTreatment(),
                assessment.isMedication(),
                assessment.isWillingContact());
    }

    public byte[] encryptReviewMetadata(Long reviewedBy, LocalDateTime reviewedAt) {
        return codec.encryptJson(new RiskReviewMetadata(reviewedBy, reviewedAt));
    }

    public RiskReviewMetadata readReviewMetadata(RiskAssessment assessment) {
        if (assessment.getReviewMetadataEncrypted() != null) {
            return codec.decryptJson(assessment.getReviewMetadataEncrypted(), RiskReviewMetadata.class);
        }
        return new RiskReviewMetadata(assessment.getReviewedBy(), assessment.getReviewedAt());
    }

    public byte[] encryptText(String value) {
        return codec.encryptText(trimToNull(value));
    }

    public String readCancellationReason(Appointment appointment) {
        if (appointment.getCancelReasonEncrypted() != null) {
            return codec.decryptText(appointment.getCancelReasonEncrypted());
        }
        return appointment.getLegacyCancelReason();
    }

    public String readReferralDestination(Referral referral) {
        if (referral == null) {
            return null;
        }
        if (referral.getDestinationEncrypted() != null) {
            return codec.decryptText(referral.getDestinationEncrypted());
        }
        return referral.getLegacyDestination();
    }

    public boolean isConfigured() {
        return codec.isConfigured();
    }

    private List<String> readLegacyIssueTypes(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new IllegalArgumentException("Legacy appointment issue types cannot be decoded", exception);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
