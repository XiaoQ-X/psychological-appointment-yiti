package cn.schoolpsych.appointment.student.appointment;

import java.time.LocalDate;
import java.util.List;

import cn.schoolpsych.appointment.common.api.ApiResponse;
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
@RequestMapping("/api/student/appointments")
public class StudentAppointmentController {

    private final StudentAppointmentService studentAppointmentService;
    private final StudentAppointmentQueryService studentAppointmentQueryService;

    public StudentAppointmentController(
            StudentAppointmentService studentAppointmentService,
            StudentAppointmentQueryService studentAppointmentQueryService) {
        this.studentAppointmentService = studentAppointmentService;
        this.studentAppointmentQueryService = studentAppointmentQueryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    ApiResponse<List<StudentAppointmentRecordResponse>> list(
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal AuthenticatedAccount principal) {
        return ApiResponse.ok(studentAppointmentQueryService.list(principal, status, from, to));
    }

    @GetMapping("/{appointmentId}")
    @PreAuthorize("hasRole('STUDENT')")
    ApiResponse<StudentAppointmentDetailResponse> detail(
            @PathVariable Long appointmentId,
            @AuthenticationPrincipal AuthenticatedAccount principal) {
        return ApiResponse.ok(studentAppointmentQueryService.detail(appointmentId, principal));
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    ApiResponse<SubmitAppointmentResponse> submit(
            @Valid @RequestBody SubmitAppointmentRequest request,
            @AuthenticationPrincipal AuthenticatedAccount principal,
            HttpServletRequest httpRequest) {
        return ApiResponse.ok(studentAppointmentService.submit(request, principal, clientInfo(httpRequest)));
    }

    @PostMapping("/slots/{slotId}/lock")
    @PreAuthorize("hasRole('STUDENT')")
    ApiResponse<LockSlotResponse> lockSlot(
            @PathVariable Long slotId,
            @AuthenticationPrincipal AuthenticatedAccount principal) {
        return ApiResponse.ok(studentAppointmentService.lockSlot(slotId, principal));
    }

    @PostMapping("/{appointmentId}/cancel")
    @PreAuthorize("hasRole('STUDENT')")
    ApiResponse<CancelAppointmentResponse> cancel(
            @PathVariable Long appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request,
            @AuthenticationPrincipal AuthenticatedAccount principal) {
        return ApiResponse.ok(studentAppointmentService.cancel(appointmentId, request, principal));
    }

    private String clientInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent == null ? "unknown" : userAgent;
    }
}
