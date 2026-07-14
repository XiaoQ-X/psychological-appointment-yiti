package cn.schoolpsych.appointment.admin.rule;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.admin.audit.AuditActions;
import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.domain.rule.AppointmentRuleSet;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.AppointmentRuleSetRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAppointmentRuleService {

    private final AppointmentRuleSetRepository ruleSetRepository;
    private final AccountRepository accountRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AdminAppointmentRuleService(
            AppointmentRuleSetRepository ruleSetRepository,
            AccountRepository accountRepository,
            AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.ruleSetRepository = ruleSetRepository;
        this.accountRepository = accountRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AppointmentRuleResponse> list() {
        List<AppointmentRuleSet> ruleSets = ruleSetRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, String> usernames = usernamesByAccountId(
                ruleSets.stream().map(AppointmentRuleSet::getPublishedBy).distinct().toList());
        return ruleSets.stream().map(ruleSet -> toResponse(ruleSet, usernames)).toList();
    }

    @Transactional
    public AppointmentRuleResponse create(
            AppointmentRuleRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        validateSettings(request.settings());
        AppointmentRuleSet ruleSet = ruleSetRepository.save(AppointmentRuleSet.draft(
                request.name().trim(),
                toJson(request.settings()),
                actor.accountId()));
        auditLogService.record(
                actor,
                AuditActions.APPOINTMENT_RULE_CREATED,
                "APPOINTMENT_RULE_SET",
                ruleSet.getId(),
                SensitiveLevel.SENSITIVE,
                metadata,
                Map.of("name", ruleSet.getName(), "active", false));
        return toResponse(ruleSet, Map.of(actor.accountId(), actor.username()));
    }

    @Transactional
    public AppointmentRuleResponse update(
            Long id,
            AppointmentRuleRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        validateSettings(request.settings());
        AppointmentRuleSet ruleSet = ruleSetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment rule set not found"));
        try {
            ruleSet.updateDraft(request.name().trim(), toJson(request.settings()), actor.accountId());
        } catch (IllegalStateException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
        ruleSetRepository.save(ruleSet);
        auditLogService.record(
                actor,
                AuditActions.APPOINTMENT_RULE_UPDATED,
                "APPOINTMENT_RULE_SET",
                ruleSet.getId(),
                SensitiveLevel.SENSITIVE,
                metadata,
                Map.of("name", ruleSet.getName(), "active", false));
        return toResponse(ruleSet, Map.of(actor.accountId(), actor.username()));
    }

    @Transactional
    public AppointmentRuleResponse activate(
            Long id,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        List<AppointmentRuleSet> ruleSets = ruleSetRepository.findAllForUpdate();
        AppointmentRuleSet target = ruleSets.stream()
                .filter(ruleSet -> ruleSet.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Appointment rule set not found"));
        if (target.isActive()) {
            throw new IllegalArgumentException("Appointment rule set is already active");
        }

        LocalDateTime now = LocalDateTime.now();
        Long previousActiveRuleId = null;
        for (AppointmentRuleSet ruleSet : ruleSets) {
            if (ruleSet.isActive()) {
                previousActiveRuleId = ruleSet.getId();
                ruleSet.deactivate(now);
            }
        }
        target.activate(actor.accountId(), now);
        ruleSetRepository.saveAll(ruleSets);
        auditLogService.record(
                actor,
                AuditActions.APPOINTMENT_RULE_ACTIVATED,
                "APPOINTMENT_RULE_SET",
                target.getId(),
                SensitiveLevel.SENSITIVE,
                metadata,
                Map.of(
                        "name", target.getName(),
                        "previousActiveRuleId", previousActiveRuleId == null ? 0L : previousActiveRuleId));
        return toResponse(target, Map.of(actor.accountId(), actor.username()));
    }

    private AppointmentRuleResponse toResponse(AppointmentRuleSet ruleSet, Map<Long, String> usernames) {
        return new AppointmentRuleResponse(
                ruleSet.getId(),
                ruleSet.getName(),
                fromJson(ruleSet.getSettingsJson()),
                ruleSet.isActive(),
                ruleSet.getEffectiveFrom(),
                ruleSet.getEffectiveTo(),
                ruleSet.getPublishedBy(),
                usernames.get(ruleSet.getPublishedBy()),
                ruleSet.getCreatedAt(),
                ruleSet.getUpdatedAt(),
                ruleSet.getVersion());
    }

    private void validateSettings(AppointmentRuleSettings settings) {
        if (settings.minBookingHoursAhead() > settings.maxBookingDaysAhead() * 24) {
            throw new IllegalArgumentException("minBookingHoursAhead must fit within maxBookingDaysAhead");
        }
    }

    private String toJson(AppointmentRuleSettings settings) {
        try {
            AppointmentRuleSettings normalized = settings.noShowRestrictThreshold() == null
                    ? new AppointmentRuleSettings(
                            settings.slotGapMinutes(),
                            settings.slotLockMinutes(),
                            settings.maxBookingDaysAhead(),
                            settings.minBookingHoursAhead(),
                            settings.minCancelHoursAhead(),
                            settings.maxWeeklyAppointments(),
                            settings.maxSemesterCompletedAppointments(),
                            settings.maxActiveAppointments(),
                            AppointmentRuleSettings.defaults().noShowRestrictThreshold())
                    : settings;
            return objectMapper.writeValueAsString(normalized);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Appointment rule settings are invalid", exception);
        }
    }

    private AppointmentRuleSettings fromJson(String settingsJson) {
        AppointmentRuleSettings defaults = AppointmentRuleSettings.defaults();
        try {
            JsonNode settings = objectMapper.readTree(settingsJson);
            return new AppointmentRuleSettings(
                    settings.path("slotGapMinutes").asInt(defaults.slotGapMinutes()),
                    settings.path("slotLockMinutes").asInt(defaults.slotLockMinutes()),
                    settings.path("maxBookingDaysAhead").asInt(defaults.maxBookingDaysAhead()),
                    settings.path("minBookingHoursAhead").asInt(defaults.minBookingHoursAhead()),
                    settings.path("minCancelHoursAhead").asInt(defaults.minCancelHoursAhead()),
                    settings.path("maxWeeklyAppointments").asInt(defaults.maxWeeklyAppointments()),
                    settings.path("maxSemesterCompletedAppointments").asInt(defaults.maxSemesterCompletedAppointments()),
                    settings.path("maxActiveAppointments").asInt(defaults.maxActiveAppointments()),
                    settings.path("noShowRestrictThreshold").asInt(defaults.noShowRestrictThreshold()));
        } catch (JsonProcessingException exception) {
            return defaults;
        }
    }

    private Map<Long, String> usernamesByAccountId(Collection<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Account::getUsername));
    }
}
