package cn.schoolpsych.appointment.student.auth;

import cn.schoolpsych.appointment.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/auth")
public class StudentAuthController {

    private final StudentAuthService studentAuthService;

    public StudentAuthController(StudentAuthService studentAuthService) {
        this.studentAuthService = studentAuthService;
    }

    @PostMapping("/login")
    ApiResponse<StudentLoginResponse> login(@Valid @RequestBody StudentLoginRequest request) {
        return ApiResponse.ok(studentAuthService.login(request));
    }
}
