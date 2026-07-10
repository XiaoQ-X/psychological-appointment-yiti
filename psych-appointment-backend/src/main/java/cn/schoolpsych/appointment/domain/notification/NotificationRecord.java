package cn.schoolpsych.appointment.domain.notification;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_records")
public class NotificationRecord extends BaseEntity {

    @Column(name = "recipient_account_id", nullable = false)
    private Long recipientAccountId;

    @Column(name = "appointment_id")
    private Long appointmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 32)
    private NotificationChannel channel;

    @Column(name = "template_code", nullable = false, length = 128)
    private String templateCode;

    @Column(name = "content_json", nullable = false, columnDefinition = "json")
    private String contentJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private NotificationStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "fail_reason", length = 500)
    private String failReason;

    protected NotificationRecord() {
    }
}
