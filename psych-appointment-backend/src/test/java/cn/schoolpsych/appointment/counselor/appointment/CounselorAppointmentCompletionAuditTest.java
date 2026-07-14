package cn.schoolpsych.appointment.counselor.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import cn.schoolpsych.appointment.admin.audit.AuditActions;
import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentSlotSnapshot;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.domain.counselor.Counselor;
import cn.schoolpsych.appointment.domain.note.ConsultationNote;
import cn.schoolpsych.appointment.repository.AppointmentFormRepository;
import cn.schoolpsych.appointment.repository.AppointmentRepository;
import cn.schoolpsych.appointment.repository.CampusRepository;
import cn.schoolpsych.appointment.repository.ConsultationNoteRepository;
import cn.schoolpsych.appointment.repository.CounselorRepository;
import cn.schoolpsych.appointment.repository.RoomRepository;
import cn.schoolpsych.appointment.repository.ServiceTypeRepository;
import cn.schoolpsych.appointment.repository.StudentRepository;
import cn.schoolpsych.appointment.security.AppointmentSensitiveDataService;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import cn.schoolpsych.appointment.security.SensitiveDataEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class CounselorAppointmentCompletionAuditTest {

    private CounselorRepository counselorRepository;
    private AppointmentRepository appointmentRepository;
    private ConsultationNoteRepository consultationNoteRepository;
    private AuditLogService auditLogService;
    private CounselorAppointmentService service;
    private Appointment appointment;

    @BeforeEach
    void setUp() {
        counselorRepository = mock(CounselorRepository.class);
        appointmentRepository = mock(AppointmentRepository.class);
        consultationNoteRepository = mock(ConsultationNoteRepository.class);
        auditLogService = mock(AuditLogService.class);

        Counselor counselor = Counselor.create(
                88L, "Counselor", null, null, null, null,
                null, null, null, null, 8, true, "ACTIVE");
        ReflectionTestUtils.setField(counselor, "id", 200L);
        appointment = appointment();

        when(counselorRepository.findByAccountId(88L)).thenReturn(Optional.of(counselor));
        when(appointmentRepository.findByIdAndCounselorIdForUpdate(100L, 200L))
                .thenReturn(Optional.of(appointment));
        when(consultationNoteRepository.existsByAppointmentId(100L)).thenReturn(false);
        when(consultationNoteRepository.save(any(ConsultationNote.class))).thenAnswer(invocation -> {
            ConsultationNote note = invocation.getArgument(0);
            ReflectionTestUtils.setField(note, "id", 300L);
            return note;
        });

        byte[] key = new byte[32];
        SensitiveDataEncryptor encryptor = new SensitiveDataEncryptor(
                Base64.getEncoder().encodeToString(key));
        service = new CounselorAppointmentService(
                counselorRepository,
                appointmentRepository,
                mock(AppointmentFormRepository.class),
                consultationNoteRepository,
                mock(StudentRepository.class),
                mock(CampusRepository.class),
                mock(RoomRepository.class),
                mock(ServiceTypeRepository.class),
                encryptor,
                mock(AppointmentSensitiveDataService.class),
                auditLogService);
    }

    @Test
    void completionWritesExactlyOneSensitiveAuditWithoutNotePlaintext() {
        CompleteAppointmentRequest request = request();

        CompleteAppointmentResponse response = service.complete(100L, request, actor(), metadata());

        assertThat(response.appointmentStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        ArgumentCaptor<Map<String, ?>> detailCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditLogService).record(
                eq(actor()),
                eq(AuditActions.APPOINTMENT_COMPLETED),
                eq("APPOINTMENT"),
                eq(100L),
                eq(SensitiveLevel.SENSITIVE),
                eq(metadata()),
                detailCaptor.capture());
        String detail = detailCaptor.getValue().toString();
        assertThat(detail).contains("needReferral=true", "riskChange=MEDIUM", "noteId=300");
        assertThat(detail).doesNotContain(request.topic(), request.summary(), request.followUpPlan());

        assertThatThrownBy(() -> service.complete(100L, request, actor(), metadata()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be completed");
        verify(auditLogService, times(1)).record(
                any(AuthenticatedAccount.class),
                any(String.class),
                any(String.class),
                any(Long.class),
                any(SensitiveLevel.class),
                any(AuditRequestMetadata.class),
                any(Map.class));
    }

    @Test
    void auditFailureIsPropagatedSoTheCompletionTransactionCanRollBack() {
        doThrow(new IllegalStateException("audit write failed"))
                .when(auditLogService)
                .record(
                        any(AuthenticatedAccount.class),
                        any(String.class),
                        any(String.class),
                        any(Long.class),
                        any(SensitiveLevel.class),
                        any(AuditRequestMetadata.class),
                        any(Map.class));

        assertThatThrownBy(() -> service.complete(100L, request(), actor(), metadata()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("audit write failed");
    }

    private Appointment appointment() {
        LocalDateTime startAt = LocalDateTime.now().minusHours(2);
        Appointment result = Appointment.create(
                "APT-audit-complete",
                400L,
                new AppointmentSlotSnapshot(
                        500L, 200L, 600L, 700L, 800L, startAt, startAt.plusMinutes(50)),
                900L,
                1000L,
                1100L,
                AppointmentStatus.CONFIRMED,
                RiskLevel.LOW);
        ReflectionTestUtils.setField(result, "id", 100L);
        return result;
    }

    private CompleteAppointmentRequest request() {
        return new CompleteAppointmentRequest(
                "private-topic-marker",
                "private-summary-marker",
                "medium",
                "private-follow-up-marker",
                true);
    }

    private AuthenticatedAccount actor() {
        return new AuthenticatedAccount(88L, "counselor", AccountRole.COUNSELOR);
    }

    private AuditRequestMetadata metadata() {
        return new AuditRequestMetadata("127.0.0.1", "audit-test-agent");
    }
}
