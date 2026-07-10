package cn.schoolpsych.appointment.domain.appointment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AppointmentCompletionTest {

    @Test
    void confirmedAppointmentCanBeCompleted() {
        Appointment appointment = appointment(AppointmentStatus.CONFIRMED);

        assertThat(appointment.canBeCompletedByCounselor()).isTrue();

        appointment.complete();

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
        assertThat(appointment.getCompletedAt()).isNotNull();
    }

    @Test
    void canceledAppointmentCannotBeCompleted() {
        Appointment appointment = appointment(AppointmentStatus.CANCELED_BY_STUDENT);

        assertThat(appointment.canBeCompletedByCounselor()).isFalse();
    }

    private Appointment appointment(AppointmentStatus status) {
        LocalDateTime startAt = LocalDateTime.now().plusDays(1);
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
