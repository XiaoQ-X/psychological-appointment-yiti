package cn.schoolpsych.appointment.domain.schedule;

import java.time.LocalDate;
import java.time.LocalTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "counselor_schedule_templates")
public class CounselorScheduleTemplate extends BaseEntity {

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Column(name = "campus_id", nullable = false)
    private Long campusId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "service_type_id", nullable = false)
    private Long serviceTypeId;

    @Column(name = "day_of_week", nullable = false)
    private byte dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected CounselorScheduleTemplate() {
    }

    public static CounselorScheduleTemplate create(
            Long counselorId,
            Long campusId,
            Long roomId,
            Long serviceTypeId,
            byte dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String status) {
        CounselorScheduleTemplate template = new CounselorScheduleTemplate();
        template.update(
                counselorId,
                campusId,
                roomId,
                serviceTypeId,
                dayOfWeek,
                startTime,
                endTime,
                effectiveFrom,
                effectiveTo,
                status);
        return template;
    }

    public void update(
            Long counselorId,
            Long campusId,
            Long roomId,
            Long serviceTypeId,
            byte dayOfWeek,
            LocalTime startTime,
            LocalTime endTime,
            LocalDate effectiveFrom,
            LocalDate effectiveTo,
            String status) {
        this.counselorId = counselorId;
        this.campusId = campusId;
        this.roomId = roomId;
        this.serviceTypeId = serviceTypeId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.effectiveFrom = effectiveFrom;
        this.effectiveTo = effectiveTo;
        this.status = status;
    }

    public boolean appliesTo(LocalDate date) {
        return status.equals("ACTIVE")
                && !effectiveFrom.isAfter(date)
                && (effectiveTo == null || !effectiveTo.isBefore(date));
    }

    public Long getCounselorId() {
        return counselorId;
    }

    public Long getCampusId() {
        return campusId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getServiceTypeId() {
        return serviceTypeId;
    }

    public byte getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public String getStatus() {
        return status;
    }
}
