package cn.schoolpsych.appointment.api;

import cn.schoolpsych.appointment.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/consent")
public class PublicConsentController {

    private final PublicConsentService publicConsentService;

    public PublicConsentController(PublicConsentService publicConsentService) {
        this.publicConsentService = publicConsentService;
    }

    @GetMapping("/current")
    ApiResponse<PublicConsentResponse> current() {
        return ApiResponse.ok(publicConsentService.current());
    }
}
