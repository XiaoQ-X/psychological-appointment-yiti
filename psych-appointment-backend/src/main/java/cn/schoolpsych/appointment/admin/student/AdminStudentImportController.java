package cn.schoolpsych.appointment.admin.student;

import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.common.api.ApiResponse;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/admin/students")
public class AdminStudentImportController {

    private final StudentImportService studentImportService;

    public AdminStudentImportController(StudentImportService studentImportService) {
        this.studentImportService = studentImportService;
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    ApiResponse<StudentImportResponse> importStudents(
            @RequestParam("file") @NotNull MultipartFile file,
            @RequestParam(name = "strategy", defaultValue = "SKIP_EXISTING") StudentImportStrategy strategy,
            @AuthenticationPrincipal AuthenticatedAccount operator,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(studentImportService.importStudents(
                file, strategy, operator, AuditRequestMetadata.from(servletRequest)));
    }
}
