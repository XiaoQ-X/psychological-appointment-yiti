package cn.schoolpsych.appointment.admin.audit;

import java.time.LocalDateTime;
import java.util.Map;

import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;

public record AuditLogResponse(
        Long id,
        Long actorAccountId,
        String actorUsername,
        String action,
        String targetType,
        Long targetId,
        SensitiveLevel sensitiveLevel,
        String ip,
        String userAgent,
        Map<String, Object> detail,
        LocalDateTime createdAt) {
}
