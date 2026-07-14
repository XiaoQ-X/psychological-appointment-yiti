package cn.schoolpsych.appointment.counselor.auth;

import cn.schoolpsych.appointment.common.api.ApiResponse;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.security.AccountPasswordService;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import cn.schoolpsych.appointment.security.ChangePasswordRequest;
import cn.schoolpsych.appointment.security.ChangePasswordResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/counselor/auth")
public class CounselorAuthController {

    private final CounselorAuthService counselorAuthService;
    private final AccountPasswordService accountPasswordService;

    public CounselorAuthController(CounselorAuthService counselorAuthService, AccountPasswordService accountPasswordService) {
        this.counselorAuthService = counselorAuthService;
        this.accountPasswordService = accountPasswordService;
    }

    @PostMapping("/login")
    ApiResponse<CounselorLoginResponse> login(@Valid @RequestBody CounselorLoginRequest request) {
        return ApiResponse.ok(counselorAuthService.login(request));
    }

    @PostMapping("/change-password")
    ApiResponse<ChangePasswordResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthenticatedAccount principal) {
        return ApiResponse.ok(accountPasswordService.changePassword(principal, AccountRole.COUNSELOR, request));
    }
}
