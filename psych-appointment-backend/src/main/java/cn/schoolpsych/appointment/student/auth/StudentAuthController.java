package cn.schoolpsych.appointment.student.auth;

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
@RequestMapping("/api/student/auth")
public class StudentAuthController {

    private final StudentAuthService studentAuthService;
    private final AccountPasswordService accountPasswordService;

    public StudentAuthController(StudentAuthService studentAuthService, AccountPasswordService accountPasswordService) {
        this.studentAuthService = studentAuthService;
        this.accountPasswordService = accountPasswordService;
    }

    @PostMapping("/login")
    ApiResponse<StudentLoginResponse> login(@Valid @RequestBody StudentLoginRequest request) {
        return ApiResponse.ok(studentAuthService.login(request));
    }

    @PostMapping("/change-password")
    ApiResponse<ChangePasswordResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal AuthenticatedAccount principal) {
        return ApiResponse.ok(accountPasswordService.changePassword(principal, AccountRole.STUDENT, request));
    }
}
