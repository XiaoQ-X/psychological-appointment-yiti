package cn.schoolpsych.appointment.counselor.auth;

import cn.schoolpsych.appointment.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/counselor/auth")
public class CounselorAuthController {

    private final CounselorAuthService counselorAuthService;

    public CounselorAuthController(CounselorAuthService counselorAuthService) {
        this.counselorAuthService = counselorAuthService;
    }

    @PostMapping("/login")
    ApiResponse<CounselorLoginResponse> login(@Valid @RequestBody CounselorLoginRequest request) {
        return ApiResponse.ok(counselorAuthService.login(request));
    }
}
