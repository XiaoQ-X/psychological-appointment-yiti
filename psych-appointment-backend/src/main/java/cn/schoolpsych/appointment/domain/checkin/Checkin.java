package cn.schoolpsych.appointment.domain.checkin;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "checkins")
public class Checkin extends BaseEntity {

    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "checkin_at", nullable = false)
    private LocalDateTime checkinAt;

    @Column(name = "method", nullable = false, length = 32)
    private String method;

    @Column(name = "late_minutes", nullable = false)
    private int lateMinutes;

    @Column(name = "operator_id")
    private Long operatorId;

    protected Checkin() {
    }
}
