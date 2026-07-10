package cn.schoolpsych.appointment.domain.appointment;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.schedule.AppointmentSlot;

public record AppointmentSlotSnapshot(
        Long slotId,
        Long counselorId,
        Long campusId,
        Long roomId,
        Long serviceTypeId,
        LocalDateTime startAt,
        LocalDateTime endAt) {

    public static AppointmentSlotSnapshot from(AppointmentSlot slot) {
        return new AppointmentSlotSnapshot(
                slot.getId(),
                slot.getCounselorId(),
                slot.getCampusId(),
                slot.getRoomId(),
                slot.getServiceTypeId(),
                slot.getStartAt(),
                slot.getEndAt());
    }
}
