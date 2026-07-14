package cn.schoolpsych.appointment.counselor.appointment;

import java.time.LocalDate;
import java.util.List;

import cn.schoolpsych.appointment.common.api.ApiResponse;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.appointment.AppointmentStatus;
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
@RequestMapping("/api/counselor/appointments")
@PreAuthorize("hasRole('COUNSELOR')")
public class CounselorAppointmentController {

    private final CounselorAppointmentService counselorAppointmentService;

    public CounselorAppointmentController(CounselorAppointmentService counselorAppointmentService) {
        this.counselorAppointmentService = counselorAppointmentService;
    }

    @GetMapping
    ApiResponse<List<CounselorAppointmentRecordResponse>> list(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedAccount principal) {
        return ApiResponse.ok(counselorAppointmentService.list(principal, status, from, to));
    }

    @GetMapping("/{appointmentId}")
    ApiResponse<CounselorAppointmentDetailResponse> detail(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal AuthenticatedAccount principal) {
        return ApiResponse.ok(counselorAppointmentService.detail(appointmentId, principal));
    }

    @PostMapping("/{appointmentId}/complete")
    ApiResponse<CompleteAppointmentResponse> complete(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CompleteAppointmentRequest request,
            @AuthenticationPrincipal AuthenticatedAccount principal,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(counselorAppointmentService.complete(
                appointmentId, request, principal, AuditRequestMetadata.from(servletRequest)));
    }

    @PostMapping("/{appointmentId}/no-show")
    ApiResponse<MarkNoShowResponse> markNoShow(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal AuthenticatedAccount principal,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(counselorAppointmentService.markNoShow(
                appointmentId, principal, AuditRequestMetadata.from(servletRequest)));
    }
}
