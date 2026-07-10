package cn.schoolpsych.appointment.student.availability;

import java.time.LocalDateTime;
import java.util.List;

public record StudentCounselorDetailResponse(
        Long id,
        String name,
        String avatarUrl,
        String title,
        String gender,
        Long campusId,
        String campusName,
        List<String> expertise,
        String intro,
        String trainingBackground,
        List<String> serviceModes,
        LocalDateTime nextAvailableAt,
        long availableSlotCount) {
}
