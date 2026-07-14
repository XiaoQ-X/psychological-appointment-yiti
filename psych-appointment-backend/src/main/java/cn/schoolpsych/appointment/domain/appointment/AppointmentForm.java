package cn.schoolpsych.appointment.domain.appointment;

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
@Table(name = "appointment_forms")
public class AppointmentForm extends BaseEntity {

    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;

    @Column(name = "first_visit", nullable = false)
    private boolean firstVisit;

    @Column(name = "issue_types_json", columnDefinition = "json")
    private String issueTypesJson;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "metadata_encrypted", nullable = false)
    private byte[] metadataEncrypted;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "description_encrypted", nullable = false)
    private byte[] descriptionEncrypted;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "expected_help_encrypted")
    private byte[] expectedHelpEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency_level", length = 32)
    private RiskLevel urgencyLevel;

    @Column(name = "contact_time", length = 255)
    private String contactTime;

    protected AppointmentForm() {
    }

    public static AppointmentForm create(
            Long appointmentId,
            boolean firstVisit,
            byte[] metadataEncrypted,
            byte[] descriptionEncrypted,
            byte[] expectedHelpEncrypted) {
        AppointmentForm form = new AppointmentForm();
        form.appointmentId = appointmentId;
        form.firstVisit = firstVisit;
        form.metadataEncrypted = metadataEncrypted;
        form.descriptionEncrypted = descriptionEncrypted;
        form.expectedHelpEncrypted = expectedHelpEncrypted;
        return form;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public boolean isFirstVisit() {
        return firstVisit;
    }

    public byte[] getMetadataEncrypted() {
        return metadataEncrypted;
    }

    public String getLegacyIssueTypesJson() {
        return issueTypesJson;
    }

    public RiskLevel getLegacyUrgencyLevel() {
        return urgencyLevel;
    }

    public String getLegacyContactTime() {
        return contactTime;
    }

    public void migrateMetadata(byte[] encryptedMetadata) {
        if (this.metadataEncrypted == null && encryptedMetadata != null) {
            this.metadataEncrypted = encryptedMetadata;
            this.issueTypesJson = null;
            this.urgencyLevel = null;
            this.contactTime = null;
        }
    }
}
