package cn.schoolpsych.appointment.domain.appointment;

import java.time.LocalDateTime;

public record RiskReviewMetadata(Long reviewedBy, LocalDateTime reviewedAt) {
}
