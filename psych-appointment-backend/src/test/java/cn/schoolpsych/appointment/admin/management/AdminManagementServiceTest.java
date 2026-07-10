package cn.schoolpsych.appointment.admin.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.counselor.Counselor;
import cn.schoolpsych.appointment.domain.rule.AppointmentRuleSet;
import cn.schoolpsych.appointment.domain.schedule.AppointmentSlot;
import cn.schoolpsych.appointment.domain.schedule.CounselorScheduleTemplate;
import cn.schoolpsych.appointment.domain.service.ServiceType;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.AppointmentRuleSetRepository;
import cn.schoolpsych.appointment.repository.AppointmentSlotRepository;
import cn.schoolpsych.appointment.repository.CampusRepository;
import cn.schoolpsych.appointment.repository.CounselorRepository;
import cn.schoolpsych.appointment.repository.CounselorScheduleTemplateRepository;
import cn.schoolpsych.appointment.repository.RoomRepository;
import cn.schoolpsych.appointment.repository.ServiceTypeRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminManagementServiceTest {

    private CampusRepository campusRepository;
    private RoomRepository roomRepository;
    private CounselorRepository counselorRepository;
    private AccountRepository accountRepository;
    private ServiceTypeRepository serviceTypeRepository;
    private CounselorScheduleTemplateRepository scheduleTemplateRepository;
    private AppointmentSlotRepository slotRepository;
    private AppointmentRuleSetRepository ruleSetRepository;
    private AuditLogService auditLogService;
    private AdminManagementService service;

    @BeforeEach
    void setUp() {
        campusRepository = mock(CampusRepository.class);
        roomRepository = mock(RoomRepository.class);
        counselorRepository = mock(CounselorRepository.class);
        accountRepository = mock(AccountRepository.class);
        serviceTypeRepository = mock(ServiceTypeRepository.class);
        scheduleTemplateRepository = mock(CounselorScheduleTemplateRepository.class);
        slotRepository = mock(AppointmentSlotRepository.class);
        ruleSetRepository = mock(AppointmentRuleSetRepository.class);
        auditLogService = mock(AuditLogService.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        service = new AdminManagementService(
                campusRepository,
                roomRepository,
                counselorRepository,
                accountRepository,
                serviceTypeRepository,
                scheduleTemplateRepository,
                slotRepository,
                ruleSetRepository,
                passwordEncoder,
                new ObjectMapper(),
                auditLogService);
    }

    @Test
    void generateSlotsUsesServiceDurationAndRuleGap() {
        LocalDate date = futureMonday();
        CounselorScheduleTemplate template = template(date, LocalTime.of(9, 0), LocalTime.of(11, 0));
        mockGenerationData(template, 0, 50, 10, 0);
        when(slotRepository.existsByCounselorIdAndStartAtAndEndAt(any(), any(), any())).thenReturn(false);

        GenerateSlotsResponse response = service.generateSlots(
                new GenerateSlotsRequest(date, date, null), actor(), metadata());

        assertThat(response.generatedCount()).isEqualTo(2);
        List<AppointmentSlot> saved = capturedSavedSlots();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getStartAt().toLocalTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(saved.get(0).getEndAt().toLocalTime()).isEqualTo(LocalTime.of(9, 50));
        assertThat(saved.get(1).getStartAt().toLocalTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(saved.get(1).getEndAt().toLocalTime()).isEqualTo(LocalTime.of(10, 50));
    }

    @Test
    void generateSlotsRespectsCounselorDailyLimit() {
        LocalDate date = futureMonday();
        CounselorScheduleTemplate template = template(date, LocalTime.of(9, 0), LocalTime.of(11, 0));
        mockGenerationData(template, 1, 50, 10, 0);
        when(slotRepository.existsByCounselorIdAndStartAtAndEndAt(any(), any(), any())).thenReturn(false);

        GenerateSlotsResponse response = service.generateSlots(
                new GenerateSlotsRequest(date, date, null), actor(), metadata());

        assertThat(response.generatedCount()).isEqualTo(1);
        assertThat(response.skippedLimitCount()).isEqualTo(1);
        assertThat(capturedSavedSlots()).hasSize(1);
    }

    private void mockGenerationData(
            CounselorScheduleTemplate template,
            int maxDailyCount,
            int durationMinutes,
            int slotGapMinutes,
            long existingDailyCount) {
        Counselor counselor = mock(Counselor.class);
        when(counselor.getId()).thenReturn(7L);
        when(counselor.getStatus()).thenReturn("ACTIVE");
        when(counselor.getMaxDailyCount()).thenReturn(maxDailyCount);

        ServiceType serviceType = mock(ServiceType.class);
        when(serviceType.getId()).thenReturn(1L);
        when(serviceType.isEnabled()).thenReturn(true);
        when(serviceType.getDurationMinutes()).thenReturn(durationMinutes);

        AppointmentRuleSet ruleSet = mock(AppointmentRuleSet.class);
        when(ruleSet.getSettingsJson()).thenReturn("{\"slotGapMinutes\":" + slotGapMinutes + "}");

        when(scheduleTemplateRepository.findActiveForGeneration(any(), any(), any(), any())).thenReturn(List.of(template));
        when(counselorRepository.findAllById(any())).thenReturn(List.of(counselor));
        when(serviceTypeRepository.findAllById(any())).thenReturn(List.of(serviceType));
        when(ruleSetRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()).thenReturn(Optional.of(ruleSet));
        when(slotRepository.countByCounselorIdAndStartAtGreaterThanEqualAndStartAtLessThan(any(), any(), any()))
                .thenReturn(existingDailyCount);
    }

    private CounselorScheduleTemplate template(LocalDate date, LocalTime startTime, LocalTime endTime) {
        return CounselorScheduleTemplate.create(
                7L,
                3L,
                5L,
                1L,
                (byte) date.getDayOfWeek().getValue(),
                startTime,
                endTime,
                date.minusDays(1),
                date.plusDays(1),
                "ACTIVE");
    }

    private LocalDate futureMonday() {
        return LocalDate.now()
                .plusWeeks(1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    private List<AppointmentSlot> capturedSavedSlots() {
        ArgumentCaptor<Iterable<AppointmentSlot>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(slotRepository).saveAll(captor.capture());
        return StreamSupport.stream(captor.getValue().spliterator(), false).toList();
    }

    private AuthenticatedAccount actor() {
        return new AuthenticatedAccount(1L, "admin", AccountRole.ADMIN);
    }

    private AuditRequestMetadata metadata() {
        return new AuditRequestMetadata("127.0.0.1", "test");
    }
}
