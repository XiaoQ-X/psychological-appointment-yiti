package cn.schoolpsych.appointment.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AppointmentCompletionTest {

    @Test
    void confirmedAppointmentCanBeCompleted() {
        LocalDateTime now = LocalDateTime.now();
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED, now.minusHours(1));

        assertThat(appointment.canBeCompletedByCounselor(now)).isTrue();

        appointment.complete(now);

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getCompletedAt()).isNotNull();
    }

    @Test
    void canceledAppointmentCannotBeCompleted() {
        LocalDateTime now = LocalDateTime.now();
        Appointment appointment = appointment(AppointmentStatus.CANCELED_BY_STUDENT, now.minusHours(1));

        assertThat(appointment.canBeCompletedByCounselor(now)).isFalse();
    }

    @Test
    void futureAppointmentCannotBeCompleted() {
        LocalDateTime now = LocalDateTime.now();
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED, now.plusHours(1));

        assertThat(appointment.canBeCompletedByCounselor(now)).isFalse();
    }

    private Appointment appointment(AppointmentStatus status, LocalDateTime startAt) {
        return Appointment.create(
                "APT-test",
                1L,
                new AppointmentSlotSnapshot(2L, 3L, 4L, 5L, 6L, startAt, startAt.plusMinutes(50)),
                7L,
                8L,
                9L,
                status,
                RiskLevel.LOW);
    }
}
