package cn.schoolpsych.appointment.student.availability;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.schedule.SlotStatus;

public record StudentSlotResponse(
        Long id,
        Long counselorId,
        String counselorName,
        Long campusId,
        String campusName,
        Long roomId,
        String roomName,
        Long serviceTypeId,
        String serviceTypeName,
        int durationMinutes,
        LocalDateTime startAt,
        LocalDateTime endAt,
        SlotStatus status) {
}
