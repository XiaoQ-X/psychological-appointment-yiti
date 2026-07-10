package cn.schoolpsych.appointment.repository;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.audit.AuditLog;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            select log from AuditLog log
            where (:actorAccountId is null or log.actorAccountId = :actorAccountId)
              and (:action is null or log.action = :action)
              and (:targetType is null or log.targetType = :targetType)
              and (:sensitiveLevel is null or log.sensitiveLevel = :sensitiveLevel)
              and (:fromAt is null or log.createdAt >= :fromAt)
              and (:toAt is null or log.createdAt < :toAt)
            """)
    Page<AuditLog> findAuditLogs(
            @Param("actorAccountId") Long actorAccountId,
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("sensitiveLevel") SensitiveLevel sensitiveLevel,
            @Param("fromAt") LocalDateTime fromAt,
            @Param("toAt") LocalDateTime toAt,
            Pageable pageable);
}
