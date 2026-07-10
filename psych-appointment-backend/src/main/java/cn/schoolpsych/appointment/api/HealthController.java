package cn.schoolpsych.appointment.api;

import java.time.OffsetDateTime;

import cn.schoolpsych.appointment.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    ApiResponse<HealthPayload> health() {
        return ApiResponse.ok(new HealthPayload("UP", OffsetDateTime.now()));
    }

    record HealthPayload(String status, OffsetDateTime time) {
    }
}
