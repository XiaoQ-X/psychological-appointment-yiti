package cn.schoolpsych.appointment.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AppointmentNoShowTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 13, 10, 0);

    @Test
    void endedConfirmedAppointmentCanBeMarkedNoShow() {
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED, NOW.minusHours(2));

        assertThat(appointment.canBeMarkedNoShow(NOW)).isTrue();

        appointment.markNoShow(NOW);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.NO_SHOW);
    }

    @Test
    void futureAppointmentCannotBeMarkedNoShow() {
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED, NOW.plusHours(2));

        assertThat(appointment.canBeMarkedNoShow(NOW)).isFalse();
        assertThatThrownBy(() -> appointment.markNoShow(NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void checkedInAppointmentCannotBeMarkedNoShow() {
        Appointment appointment = appointment(AppointmentStatus.CHECKED_IN, NOW.minusHours(2));

        assertThat(appointment.canBeMarkedNoShow(NOW)).isFalse();
    }

    private Appointment appointment(AppointmentStatus status, LocalDateTime startAt) {
        return Appointment.create(
                "APT-no-show",
                1L,
                new AppointmentSlotSnapshot(2L, 3L, 4L, 5L, 6L, startAt, startAt.plusMinutes(50)),
                7L,
                8L,
                9L,
                status,
                RiskLevel.LOW);
    }
}
