package cn.schoolpsych.appointment.domain.consent;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "consent_records")
public class ConsentRecord extends BaseEntity {

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "consent_version_id", nullable = false)
    private Long consentVersionId;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    @Column(name = "client_info", length = 255)
    private String clientInfo;

    protected ConsentRecord() {
    }

    public static ConsentRecord create(Long studentId, Long consentVersionId, String clientInfo) {
        ConsentRecord record = new ConsentRecord();
        record.studentId = studentId;
        record.consentVersionId = consentVersionId;
        record.agreedAt = LocalDateTime.now();
        record.clientInfo = clientInfo;
        return record;
    }
}
