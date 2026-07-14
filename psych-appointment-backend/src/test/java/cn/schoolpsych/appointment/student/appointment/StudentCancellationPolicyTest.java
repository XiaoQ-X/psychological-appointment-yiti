package cn.schoolpsych.appointment.student.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.domain.appointment.AppointmentSlotSnapshot;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.domain.rule.AppointmentRuleSet;
import cn.schoolpsych.appointment.repository.AppointmentRuleSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StudentCancellationPolicyTest {

    private AppointmentRuleSetRepository ruleSetRepository;
    private StudentCancellationPolicy policy;

    @BeforeEach
    void setUp() {
        ruleSetRepository = mock(AppointmentRuleSetRepository.class);
        policy = new StudentCancellationPolicy(ruleSetRepository, new ObjectMapper());
    }

    @Test
    void evaluatesCancellationUsingActiveRuleDeadline() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 10, 0);
        AppointmentRuleSet ruleSet = AppointmentRuleSet.draft(
                "36 hour cancellation",
                "{\"minCancelHoursAhead\":36}",
                1L);
        when(ruleSetRepository.findFirstByActiveTrueOrderByEffectiveFromDesc())
                .thenReturn(Optional.of(ruleSet));

        StudentCancellationPolicy.CancellationAvailability available =
                policy.evaluate(appointment(now.plusHours(48)), now);
        StudentCancellationPolicy.CancellationAvailability atDeadline =
                policy.evaluate(appointment(now.plusHours(36)), now);
        StudentCancellationPolicy.CancellationAvailability closed =
                policy.evaluate(appointment(now.plusHours(35)), now);

        assertThat(available.canCancel()).isTrue();
        assertThat(available.minCancelHoursAhead()).isEqualTo(36);
        assertThat(available.cancelDeadline()).isEqualTo(now.plusHours(12));
        assertThat(atDeadline.canCancel()).isTrue();
        assertThat(closed.canCancel()).isFalse();
    }

    @Test
    void fallsBackToTwentyFourHoursWhenNoActiveRuleExists() {
        LocalDateTime now = LocalDateTime.of(2026, 7, 10, 10, 0);
        when(ruleSetRepository.findFirstByActiveTrueOrderByEffectiveFromDesc())
                .thenReturn(Optional.empty());

        StudentCancellationPolicy.CancellationAvailability result =
                policy.evaluate(appointment(now.plusHours(30)), now);

        assertThat(result.canCancel()).isTrue();
        assertThat(result.minCancelHoursAhead()).isEqualTo(24);
        assertThat(result.cancelDeadline()).isEqualTo(now.plusHours(6));
    }

    private Appointment appointment(LocalDateTime startAt) {
        return Appointment.create(
                "APT-TEST",
                1L,
                new AppointmentSlotSnapshot(1L, 2L, 3L, 4L, 5L, startAt, startAt.plusHours(1)),
                1L,
                1L,
                1L,
                AppointmentStatus.CONFIRMED,
                RiskLevel.LOW);
    }
}
