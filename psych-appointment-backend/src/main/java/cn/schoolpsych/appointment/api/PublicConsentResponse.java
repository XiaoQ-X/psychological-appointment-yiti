package cn.schoolpsych.appointment.api;

import java.time.LocalDateTime;

public record PublicConsentResponse(
        Long id,
        String versionNo,
        String title,
        String content,
        LocalDateTime publishedAt) {
}
