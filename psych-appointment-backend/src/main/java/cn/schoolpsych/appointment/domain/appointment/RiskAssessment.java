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
    @Column(name = "handling_notes_encrypted")
    private byte[] handlingNotesEncrypted;

    protected RiskAssessment() {
    }

    public static RiskAssessment create(
            Long appointmentId,
            boolean selfHarm,
            boolean harmOthers,
            boolean crisisEvent,
            boolean psychiatricTreatment,
            boolean medication,
            boolean willingContact,
            RiskLevel riskLevel) {
        RiskAssessment assessment = new RiskAssessment();
        assessment.appointmentId = appointmentId;
        assessment.selfHarm = selfHarm;
        assessment.harmOthers = harmOthers;
        assessment.crisisEvent = crisisEvent;
        assessment.psychiatricTreatment = psychiatricTreatment;
        assessment.medication = medication;
        assessment.willingContact = willingContact;
        assessment.riskLevel = riskLevel;
        assessment.reviewStatus = riskLevel == RiskLevel.HIGH ? RiskReviewStatus.PENDING : RiskReviewStatus.NONE;
        return assessment;
    }

    public void review(RiskReviewStatus reviewStatus, Long reviewerAccountId, byte[] handlingNotesEncrypted) {
        this.reviewStatus = reviewStatus;
        this.reviewedBy = reviewerAccountId;
        this.reviewedAt = LocalDateTime.now();
        this.handlingNotesEncrypted = handlingNotesEncrypted;
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
}
