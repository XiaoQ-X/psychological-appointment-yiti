package cn.schoolpsych.appointment.domain.schedule;

import java.time.LocalDateTime;

import cn.schoolpsych.appointment.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "counselor_unavailable_periods")
public class CounselorUnavailablePeriod extends BaseEntity {

    @Column(name = "counselor_id", nullable = false)
    private Long counselorId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "status", nullable = false, length = 32)
    private String status;

    protected CounselorUnavailablePeriod() {
    }
}
