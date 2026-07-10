package cn.schoolpsych.appointment.admin.rule;

import java.util.List;

import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.common.api.ApiResponse;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/appointment-rules")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminAppointmentRuleController {

    private final AdminAppointmentRuleService ruleService;

    public AdminAppointmentRuleController(AdminAppointmentRuleService ruleService) {
        this.ruleService = ruleService;
    }

    @GetMapping
    ApiResponse<List<AppointmentRuleResponse>> list() {
        return ApiResponse.ok(ruleService.list());
    }

    @PostMapping
    ApiResponse<AppointmentRuleResponse> create(
            @Valid @RequestBody AppointmentRuleRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(ruleService.create(request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @PutMapping("/{id}")
    ApiResponse<AppointmentRuleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentRuleRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(ruleService.update(id, request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @PostMapping("/{id}/activate")
    ApiResponse<AppointmentRuleResponse> activate(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(ruleService.activate(id, actor, AuditRequestMetadata.from(servletRequest)));
    }
}
