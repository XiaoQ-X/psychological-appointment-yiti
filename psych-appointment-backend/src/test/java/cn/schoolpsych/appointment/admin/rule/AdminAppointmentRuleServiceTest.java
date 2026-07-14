package cn.schoolpsych.appointment.admin.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.admin.audit.AuditActions;
import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.domain.rule.AppointmentRuleSet;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.AppointmentRuleSetRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class AdminAppointmentRuleServiceTest {

    private AppointmentRuleSetRepository ruleSetRepository;
    private AuditLogService auditLogService;
    private ObjectMapper objectMapper;
    private AdminAppointmentRuleService service;

    @BeforeEach
    void setUp() {
        ruleSetRepository = mock(AppointmentRuleSetRepository.class);
        AccountRepository accountRepository = mock(AccountRepository.class);
        auditLogService = mock(AuditLogService.class);
        objectMapper = new ObjectMapper();
        service = new AdminAppointmentRuleService(
                ruleSetRepository, accountRepository, auditLogService, objectMapper);
    }

    @Test
    void createPersistsCompleteDraftAndAuditsIt() throws Exception {
        AppointmentRuleRequest request = new AppointmentRuleRequest(
                "New rules",
                new AppointmentRuleSettings(15, 8, 21, 12, 18, 2, 10, 2, 3));
        when(ruleSetRepository.save(any(AppointmentRuleSet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentRuleResponse response = service.create(request, actor(), metadata());

        ArgumentCaptor<AppointmentRuleSet> captor = ArgumentCaptor.forClass(AppointmentRuleSet.class);
        verify(ruleSetRepository).save(captor.capture());
        assertThat(response.active()).isFalse();
        assertThat(response.settings()).isEqualTo(request.settings());
        assertThat(objectMapper.readTree(captor.getValue().getSettingsJson()).path("slotLockMinutes").asInt())
                .isEqualTo(8);
        assertThat(objectMapper.readTree(captor.getValue().getSettingsJson()).path("noShowRestrictThreshold").asInt())
                .isEqualTo(3);
        verify(auditLogService).record(
                eq(actor()),
                eq(AuditActions.APPOINTMENT_RULE_CREATED),
                eq("APPOINTMENT_RULE_SET"),
                eq(null),
                eq(SensitiveLevel.SENSITIVE),
                eq(metadata()),
                any());
    }

    @Test
    void activateClosesPreviousRuleAndActivatesTarget() {
        AppointmentRuleSet previous = ruleSet(1L, "Previous");
        previous.activate(1L, LocalDateTime.now().minusDays(2));
        AppointmentRuleSet target = ruleSet(2L, "Target");
        when(ruleSetRepository.findAllForUpdate()).thenReturn(List.of(previous, target));

        AppointmentRuleResponse response = service.activate(2L, actor(), metadata());

        assertThat(previous.isActive()).isFalse();
        assertThat(previous.getEffectiveTo()).isNotNull();
        assertThat(target.isActive()).isTrue();
        assertThat(response.id()).isEqualTo(2L);
        verify(ruleSetRepository).saveAll(List.of(previous, target));
        verify(auditLogService).record(
                eq(actor()),
                eq(AuditActions.APPOINTMENT_RULE_ACTIVATED),
                eq("APPOINTMENT_RULE_SET"),
                eq(2L),
                eq(SensitiveLevel.SENSITIVE),
                eq(metadata()),
                any());
    }

    @Test
    void legacyRuleWithoutNoShowThresholdUsesTheDocumentedDefault() {
        AppointmentRuleSet legacy = ruleSet(3L, "Legacy");
        when(ruleSetRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(legacy));

        AppointmentRuleResponse response = service.list().get(0);

        assertThat(response.settings().noShowRestrictThreshold()).isEqualTo(2);
    }

    @Test
    void legacyRequestWithoutNoShowThresholdIsNormalizedToDefault() throws Exception {
        AppointmentRuleRequest request = new AppointmentRuleRequest(
                "Legacy client rules",
                new AppointmentRuleSettings(10, 10, 14, 24, 24, 1, 8, 1, null));
        when(ruleSetRepository.save(any(AppointmentRuleSet.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentRuleResponse response = service.create(request, actor(), metadata());

        assertThat(response.settings().noShowRestrictThreshold()).isEqualTo(2);
    }

    @Test
    void createRejectsLeadTimeOutsideBookingWindow() {
        AppointmentRuleRequest request = new AppointmentRuleRequest(
                "Invalid rules",
                new AppointmentRuleSettings(10, 10, 1, 25, 24, 1, 8, 1, 2));

        assertThatThrownBy(() -> service.create(request, actor(), metadata()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minBookingHoursAhead");
    }

    @Test
    void updateRejectsHistoricalPublishedRule() {
        AppointmentRuleSet historical = ruleSet(4L, "Historical");
        historical.activate(1L, LocalDateTime.now().minusDays(2));
        historical.deactivate(LocalDateTime.now().minusDays(1));
        when(ruleSetRepository.findById(4L)).thenReturn(java.util.Optional.of(historical));
        AppointmentRuleRequest request = new AppointmentRuleRequest(
                "Changed",
                AppointmentRuleSettings.defaults());

        assertThatThrownBy(() -> service.update(4L, request, actor(), metadata()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Published appointment rules");
    }

    private AppointmentRuleSet ruleSet(Long id, String name) {
        AppointmentRuleSet ruleSet = AppointmentRuleSet.draft(
                name,
                "{\"slotGapMinutes\":10,\"slotLockMinutes\":10,\"maxBookingDaysAhead\":14,"
                        + "\"minBookingHoursAhead\":24,\"minCancelHoursAhead\":24,"
                        + "\"maxWeeklyAppointments\":1,\"maxSemesterCompletedAppointments\":8,"
                        + "\"maxActiveAppointments\":1}",
                1L);
        ReflectionTestUtils.setField(ruleSet, "id", id);
        return ruleSet;
    }

    private AuthenticatedAccount actor() {
        return new AuthenticatedAccount(9L, "admin", AccountRole.ADMIN);
    }

    private AuditRequestMetadata metadata() {
        return new AuditRequestMetadata("127.0.0.1", "test-agent");
    }
}
