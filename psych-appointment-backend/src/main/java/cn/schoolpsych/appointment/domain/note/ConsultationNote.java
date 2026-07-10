package cn.schoolpsych.appointment.domain.note;

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
@Table(name = "consultation_notes")
public class ConsultationNote extends BaseEntity {

    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "topic_encrypted", nullable = false)
    private byte[] topicEncrypted;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "summary_encrypted", nullable = false)
    private byte[] summaryEncrypted;

    @Column(name = "risk_change", length = 32)
    private String riskChange;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "follow_up_plan_encrypted")
    private byte[] followUpPlanEncrypted;

    @Column(name = "need_referral", nullable = false)
    private boolean needReferral;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NoteStatus status;

    protected ConsultationNote() {
    }

    public static ConsultationNote submit(
            Long appointmentId,
            Long studentId,
            Long counselorId,
            byte[] topicEncrypted,
            byte[] summaryEncrypted,
            String riskChange,
            byte[] followUpPlanEncrypted,
            boolean needReferral) {
        ConsultationNote note = new ConsultationNote();
        note.appointmentId = appointmentId;
        note.studentId = studentId;
        note.counselorId = counselorId;
        note.topicEncrypted = topicEncrypted;
        note.summaryEncrypted = summaryEncrypted;
        note.riskChange = riskChange;
        note.followUpPlanEncrypted = followUpPlanEncrypted;
        note.needReferral = needReferral;
        note.status = NoteStatus.SUBMITTED;
        return note;
    }

    public Long getAppointmentId() {
        return appointmentId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getCounselorId() {
        return counselorId;
    }

    public String getRiskChange() {
        return riskChange;
    }

    public boolean isNeedReferral() {
        return needReferral;
    }

    public NoteStatus getStatus() {
        return status;
    }
}
