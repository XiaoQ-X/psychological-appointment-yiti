package cn.schoolpsych.appointment.admin.management;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCounselorRequest(
        @NotBlank @Size(max = 64) String name,
        @Size(max = 500) String avatarUrl,
        @Size(max = 128) String title,
        @Size(max = 16) String gender,
        Long campusId,
        List<String> expertise,
        @Size(max = 5000) String intro,
        @Size(max = 5000) String trainingBackground,
        List<String> serviceModes,
        @Min(0) Integer maxDailyCount,
        Boolean visible,
        @Size(max = 32) String status) {
}
