package cn.schoolpsych.appointment.counselor.appointment;

import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;

public record MarkNoShowResponse(
        Long appointmentId,
        String appointmentNo,
        AppointmentStatus appointmentStatus,
        int studentNoShowCount) {
}
