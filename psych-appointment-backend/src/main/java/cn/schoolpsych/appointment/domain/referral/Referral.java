package cn.schoolpsych.appointment.domain.referral;

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
@Table(name = "referrals")
public class Referral extends BaseEntity {

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "counselor_id")
    private Long counselorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "referral_type", nullable = false, length = 32)
    private ReferralType referralType;

    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.LONGVARBINARY)
    @Column(name = "reason_encrypted", nullable = false)
    private byte[] reasonEncrypted;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ReferralStatus status;

    protected Referral() {
    }

    public static Referral open(
            Long appointmentId,
            Long studentId,
            Long counselorId,
            ReferralType referralType,
            String destination,
            byte[] reasonEncrypted) {
        Referral referral = new Referral();
        referral.appointmentId = appointmentId;
        referral.studentId = studentId;
        referral.counselorId = counselorId;
        referral.referralType = referralType;
        referral.destination = destination;
        referral.reasonEncrypted = reasonEncrypted;
        referral.status = ReferralStatus.OPEN;
        return referral;
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

    public ReferralType getReferralType() {
        return referralType;
    }

    public String getDestination() {
        return destination;
    }

    public ReferralStatus getStatus() {
        return status;
    }
}
