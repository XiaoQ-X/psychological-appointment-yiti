package cn.schoolpsych.appointment.domain.schedule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class AppointmentSlotTest {

    @Test
    void lockedSlotCanOnlyBeBookedByOwnerBeforeExpiry() {
        LocalDateTime now = LocalDateTime.now();
        AppointmentSlot slot = AppointmentSlot.available(
                1L,
                1L,
                1L,
                1L,
                now.plusDays(1),
                now.plusDays(1).plusMinutes(50));

        slot.markLocked(10L, now.plusMinutes(10));

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.LOCKED);
        assertThat(slot.canBeBookedBy(10L, now)).isTrue();
        assertThat(slot.canBeBookedBy(11L, now)).isFalse();
        assertThat(slot.canBeLockedBy(11L, now)).isFalse();
    }

    @Test
    void expiredLockCanBeTakenByAnotherStudent() {
        LocalDateTime now = LocalDateTime.now();
        AppointmentSlot slot = AppointmentSlot.available(
                1L,
                1L,
                1L,
                1L,
                now.plusDays(1),
                now.plusDays(1).plusMinutes(50));

        slot.markLocked(10L, now.minusMinutes(1));

        assertThat(slot.isExpiredLock(now)).isTrue();
        assertThat(slot.canBeLockedBy(11L, now)).isTrue();
    }

    @Test
    void canceledBookedSlotReturnsToAvailable() {
        LocalDateTime now = LocalDateTime.now();
        AppointmentSlot slot = AppointmentSlot.available(
                1L,
                1L,
                1L,
                1L,
                now.plusDays(1),
                now.plusDays(1).plusMinutes(50));

        slot.markBooked(99L);
        slot.releaseBooking(99L);

        assertThat(slot.getStatus()).isEqualTo(SlotStatus.AVAILABLE);
        assertThat(slot.getAppointmentId()).isNull();
    }
}
