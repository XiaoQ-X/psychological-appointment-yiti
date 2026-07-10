package cn.schoolpsych.appointment.admin.management;

import java.time.LocalDate;
import java.time.LocalTime;

import cn.schoolpsych.appointment.domain.schedule.CounselorScheduleTemplate;

public record ScheduleTemplateResponse(
        Long id,
        Long counselorId,
        String counselorName,
        Long campusId,
        String campusName,
        Long roomId,
        String roomName,
        Long serviceTypeId,
        String serviceTypeName,
        int dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate effectiveFrom,
        LocalDate effectiveTo,
        String status) {
    static ScheduleTemplateResponse from(
            CounselorScheduleTemplate template,
            String counselorName,
            String campusName,
            String roomName,
            String serviceTypeName) {
        return new ScheduleTemplateResponse(
                template.getId(),
                template.getCounselorId(),
                counselorName,
                template.getCampusId(),
                campusName,
                template.getRoomId(),
                roomName,
                template.getServiceTypeId(),
                serviceTypeName,
                template.getDayOfWeek(),
                template.getStartTime(),
                template.getEndTime(),
                template.getEffectiveFrom(),
                template.getEffectiveTo(),
                template.getStatus());
    }
}
