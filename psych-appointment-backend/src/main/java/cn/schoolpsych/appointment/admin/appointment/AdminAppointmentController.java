package cn.schoolpsych.appointment.admin.appointment;

import java.time.LocalDate;
import java.util.List;

import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.common.api.ApiResponse;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/appointments")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminAppointmentController {

    private final AdminAppointmentService adminAppointmentService;

    public AdminAppointmentController(AdminAppointmentService adminAppointmentService) {
        this.adminAppointmentService = adminAppointmentService;
    }

    @GetMapping
    ApiResponse<List<AdminAppointmentRecordResponse>> list(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) RiskLevel riskLevel,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) String studentNo,
            @RequestParam(required = false) Long counselorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(adminAppointmentService.list(status, riskLevel, studentId, studentNo, counselorId, from, to));
    }

    @GetMapping("/{appointmentId}")
    ApiResponse<AdminAppointmentDetailResponse> detail(@PathVariable Long appointmentId) {
        return ApiResponse.ok(adminAppointmentService.detail(appointmentId));
    }

    @PostMapping("/{appointmentId}/cancel")
    ApiResponse<AdminCancelAppointmentResponse> cancel(
            @PathVariable Long appointmentId,
            @Valid @RequestBody AdminCancelAppointmentRequest request,
            @AuthenticationPrincipal AuthenticatedAccount principal,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminAppointmentService.cancel(
                appointmentId, request, principal, AuditRequestMetadata.from(servletRequest)));
    }

    @PostMapping("/{appointmentId}/risk-review")
    ApiResponse<RiskReviewResponse> reviewRisk(
            @PathVariable Long appointmentId,
            @Valid @RequestBody RiskReviewRequest request,
            @AuthenticationPrincipal AuthenticatedAccount principal,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminAppointmentService.reviewRisk(
                appointmentId, request, principal, AuditRequestMetadata.from(servletRequest)));
    }
}
