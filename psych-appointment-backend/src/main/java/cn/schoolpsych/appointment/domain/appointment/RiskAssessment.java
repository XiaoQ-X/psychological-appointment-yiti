package cn.schoolpsych.appointment.domain.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment extends BaseEntity {

    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;

    @Column(name = "self_harm", nullable = false)
    private boolean selfHarm;

    @Column(name = "harm_others", nullable = false)
    private boolean harmOthers;

    @Column(name = "crisis_event", nullable = false)
    private boolean crisisEvent;

    @Column(name = "psychiatric_treatment", nullable = false)
    private boolean psychiatricTreatment;

    @Column(name = "medication", nullable = false)
    private boolean medication;

    @Column(name = "willing_contact", nullable = false)
    private boolean willingContact;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "answers_encrypted", nullable = false)
    private byte[] answersEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 32)
    private RiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 32)
    private RiskReviewStatus reviewStatus;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "review_metadata_encrypted")
    private byte[] reviewMetadataEncrypted;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "handling_notes_encrypted")
    private byte[] handlingNotesEncrypted;

    protected RiskAssessment() {
    }

    public static RiskAssessment create(
            Long appointmentId,
            byte[] answersEncrypted,
            RiskLevel riskLevel) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.appointmentId = appointmentId;
        assessment.answersEncrypted = answersEncrypted;
        assessment.riskLevel = riskLevel;
        assessment.reviewStatus = riskLevel == RiskLevel.HIGH ? RiskReviewStatus.PENDING : RiskReviewStatus.NONE;
        return assessment;
    }

    public void review(
            RiskReviewStatus reviewStatus,
            byte[] handlingNotesEncrypted,
            byte[] reviewMetadataEncrypted) {
        this.reviewStatus = reviewStatus;
        this.reviewedBy = null;
        this.reviewedAt = null;
        this.handlingNotesEncrypted = handlingNotesEncrypted;
        this.reviewMetadataEncrypted = reviewMetadataEncrypted;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public boolean isSelfHarm() {
        return selfHarm;
    }

    public boolean isHarmOthers() {
        return harmOthers;
    }

    public boolean isCrisisEvent() {
        return crisisEvent;
    }

    public boolean isPsychiatricTreatment() {
        return psychiatricTreatment;
    }

    public boolean isMedication() {
        return medication;
    }

    public boolean isWillingContact() {
        return willingContact;
    }

    public byte[] getAnswersEncrypted() {
        return answersEncrypted;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public RiskReviewStatus getReviewStatus() {
        return reviewStatus;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }

    public byte[] getReviewMetadataEncrypted() {
        return reviewMetadataEncrypted;
    }

    public void migrateAnswers(byte[] encryptedAnswers) {
        if (this.answersEncrypted == null && encryptedAnswers != null) {
            this.answersEncrypted = encryptedAnswers;
            this.selfHarm = false;
            this.harmOthers = false;
            this.crisisEvent = false;
            this.psychiatricTreatment = false;
            this.medication = false;
            this.willingContact = false;
        }
    }

    public void migrateReviewMetadata(byte[] encryptedMetadata) {
        if (this.reviewMetadataEncrypted == null && encryptedMetadata != null) {
            this.reviewMetadataEncrypted = encryptedMetadata;
            this.reviewedBy = null;
            this.reviewedAt = null;
        }
    }
}
