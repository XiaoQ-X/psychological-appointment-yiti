package cn.schoolpsych.appointment.domain.audit;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_account_id", nullable = false)
    private Long actorAccountId;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "target_type", nullable = false, length = 64)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sensitive_level", nullable = false, length = 32)
    private SensitiveLevel sensitiveLevel;

    @Column(name = "ip", length = 64)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "detail_json", nullable = false, columnDefinition = "json")
    private String detailJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AuditLog() {
    }

    public static AuditLog create(
            Long actorAccountId,
            String action,
            String targetType,
            Long targetId,
            SensitiveLevel sensitiveLevel,
            String ip,
            String userAgent,
            String detailJson) {
        AuditLog auditLog = new AuditLog();
        auditLog.actorAccountId = actorAccountId;
        auditLog.action = action;
        auditLog.targetType = targetType;
        auditLog.targetId = targetId;
        auditLog.sensitiveLevel = sensitiveLevel;
        auditLog.ip = ip;
        auditLog.userAgent = userAgent;
        auditLog.detailJson = detailJson;
        auditLog.createdAt = LocalDateTime.now();
        return auditLog;
    }

    public Long getId() {
        return id;
    }

    public Long getActorAccountId() {
        return actorAccountId;
    }

    public String getAction() {
        return action;
    }

    public String getTargetType() {
        return targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public SensitiveLevel getSensitiveLevel() {
        return sensitiveLevel;
    }

    public String getIp() {
        return ip;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
