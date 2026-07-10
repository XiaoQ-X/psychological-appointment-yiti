package cn.schoolpsych.appointment.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.audit.AuditLog;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;

class AuditLogServiceTest {

    private AuditLogRepository auditLogRepository;
    private AccountRepository accountRepository;
    private AuditLogService service;

    @BeforeEach
    void setUp() {
        auditLogRepository = mock(AuditLogRepository.class);
        accountRepository = mock(AccountRepository.class);
        service = new AuditLogService(auditLogRepository, accountRepository, new ObjectMapper());
    }

    @Test
    void recordSerializesOnlySuppliedMetadata() {
        service.record(
                3L,
                AuditActions.STUDENT_IMPORT,
                "STUDENT_IMPORT_BATCH",
                7L,
                SensitiveLevel.SENSITIVE,
                new AuditRequestMetadata("127.0.0.1", "test-agent"),
                Map.of("successCount", 5));

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertThat(saved.getActorAccountId()).isEqualTo(3L);
        assertThat(saved.getIp()).isEqualTo("127.0.0.1");
        assertThat(saved.getDetailJson()).contains("successCount").doesNotContain("password");
    }

    @Test
    void listReturnsActorNameAndParsedDetail() {
        AuditLog log = AuditLog.create(
                3L,
                AuditActions.ADMIN_LOGIN,
                "ACCOUNT",
                3L,
                SensitiveLevel.NORMAL,
                "127.0.0.1",
                "test-agent",
                "{\"role\":\"ADMIN\"}");
        when(auditLogRepository.findAuditLogs(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(log)));
        Account account = mock(Account.class);
        when(account.getId()).thenReturn(3L);
        when(account.getUsername()).thenReturn("admin");
        when(accountRepository.findAllById(any())).thenReturn(List.of(account));

        AuditLogPageResponse response = service.list(
                3L,
                AuditActions.ADMIN_LOGIN,
                "ACCOUNT",
                SensitiveLevel.NORMAL,
                LocalDate.now(),
                LocalDate.now(),
                0,
                20);

        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.items().get(0).actorUsername()).isEqualTo("admin");
        assertThat(response.items().get(0).detail()).containsEntry("role", "ADMIN");
    }
}
