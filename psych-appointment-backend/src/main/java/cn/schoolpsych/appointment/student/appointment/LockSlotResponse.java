package cn.schoolpsych.appointment.student.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.schedule.SlotStatus;

public record LockSlotResponse(
        Long slotId,
        Long counselorId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        SlotStatus status,
        LocalDateTime lockedUntil) {
}
