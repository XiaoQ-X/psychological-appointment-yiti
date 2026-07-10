package cn.schoolpsych.appointment.admin.audit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.audit.AuditLog;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.AuditLogRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(
            AuditLogRepository auditLogRepository,
            AccountRepository accountRepository,
            ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.accountRepository = accountRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            AuthenticatedAccount actor,
            String action,
            String targetType,
            Long targetId,
            SensitiveLevel sensitiveLevel,
            AuditRequestMetadata metadata,
            Map<String, ?> detail) {
        record(actor.accountId(), action, targetType, targetId, sensitiveLevel, metadata, detail);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void record(
            Long actorAccountId,
            String action,
            String targetType,
            Long targetId,
            SensitiveLevel sensitiveLevel,
            AuditRequestMetadata metadata,
            Map<String, ?> detail) {
        auditLogRepository.save(AuditLog.create(
                actorAccountId,
                action,
                targetType,
                targetId,
                sensitiveLevel,
                metadata == null ? null : metadata.ip(),
                metadata == null ? null : metadata.userAgent(),
                toJson(detail)));
    }

    @Transactional(readOnly = true)
    public AuditLogPageResponse list(
            Long actorAccountId,
            String action,
            String targetType,
            SensitiveLevel sensitiveLevel,
            LocalDate from,
            LocalDate to,
            int page,
            int size) {
        if (to != null && from != null && to.isBefore(from)) {
            throw new IllegalArgumentException("to must not be before from");
        }
        LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
        LocalDateTime toAt = to == null ? null : to.plusDays(1).atStartOfDay();
        Page<AuditLog> logs = auditLogRepository.findAuditLogs(
                actorAccountId,
                trimToNull(action),
                trimToNull(targetType),
                sensitiveLevel,
                fromAt,
                toAt,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        Map<Long, String> usernames = usernamesByAccountId(
                logs.getContent().stream().map(AuditLog::getActorAccountId).distinct().toList());
        return new AuditLogPageResponse(
                logs.getContent().stream().map(log -> toResponse(log, usernames)).toList(),
                logs.getNumber(),
                logs.getSize(),
                logs.getTotalElements(),
                logs.getTotalPages());
    }

    private AuditLogResponse toResponse(AuditLog log, Map<Long, String> usernames) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorAccountId(),
                usernames.get(log.getActorAccountId()),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getSensitiveLevel(),
                log.getIp(),
                log.getUserAgent(),
                readJson(log.getDetailJson()),
                log.getCreatedAt());
    }

    private Map<Long, String> usernamesByAccountId(Collection<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Account::getUsername));
    }

    private String toJson(Map<String, ?> detail) {
        try {
            return objectMapper.writeValueAsString(detail == null ? Map.of() : detail);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Audit detail is invalid", exception);
        }
    }

    private Map<String, Object> readJson(String detailJson) {
        try {
            return objectMapper.readValue(detailJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException exception) {
            return Map.of();
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
