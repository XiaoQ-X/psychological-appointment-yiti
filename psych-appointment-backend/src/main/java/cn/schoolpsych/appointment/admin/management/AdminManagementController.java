package cn.schoolpsych.appointment.admin.management;

import java.util.List;

import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.common.api.ApiResponse;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminManagementController {

    private final AdminManagementService adminManagementService;

    public AdminManagementController(AdminManagementService adminManagementService) {
        this.adminManagementService = adminManagementService;
    }

    @PostMapping("/campuses")
    ApiResponse<CampusResponse> createCampus(
            @Valid @RequestBody CampusRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminManagementService.createCampus(
                request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @PutMapping("/campuses/{id}")
    ApiResponse<CampusResponse> updateCampus(
            @PathVariable Long id,
            @Valid @RequestBody CampusRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminManagementService.updateCampus(
                id, request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @GetMapping("/campuses")
    ApiResponse<List<CampusResponse>> listCampuses(@RequestParam(required = false) String status) {
        return ApiResponse.ok(adminManagementService.listCampuses(status));
    }

    @PostMapping("/rooms")
    ApiResponse<RoomResponse> createRoom(
            @Valid @RequestBody RoomRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminManagementService.createRoom(
                request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @PutMapping("/rooms/{id}")
    ApiResponse<RoomResponse> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminManagementService.updateRoom(
                id, request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @GetMapping("/rooms")
    ApiResponse<List<RoomResponse>> listRooms(
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(adminManagementService.listRooms(campusId, status));
    }

    @PostMapping("/counselors")
    ApiResponse<CounselorResponse> createCounselor(
            @Valid @RequestBody CounselorRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminManagementService.createCounselor(
                request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @PutMapping("/counselors/{id}")
    ApiResponse<CounselorResponse> updateCounselor(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCounselorRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminManagementService.updateCounselor(
                id, request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @GetMapping("/counselors")
    ApiResponse<List<CounselorResponse>> listCounselors(@RequestParam(required = false) String status) {
        return ApiResponse.ok(adminManagementService.listCounselors(status));
    }

    @GetMapping("/counselors/{id}")
    ApiResponse<CounselorResponse> getCounselor(@PathVariable Long id) {
        return ApiResponse.ok(adminManagementService.getCounselor(id));
    }

    @PostMapping("/schedules")
    ApiResponse<ScheduleTemplateResponse> createSchedule(
            @Valid @RequestBody ScheduleTemplateRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminManagementService.createScheduleTemplate(
                request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @PutMapping("/schedules/{id}")
    ApiResponse<ScheduleTemplateResponse> updateSchedule(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleTemplateRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminManagementService.updateScheduleTemplate(
                id, request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @GetMapping("/schedules")
    ApiResponse<List<ScheduleTemplateResponse>> listSchedules(
            @RequestParam(required = false) Long counselorId,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(adminManagementService.listScheduleTemplates(counselorId, status));
    }

    @PostMapping("/slots/generate")
    ApiResponse<GenerateSlotsResponse> generateSlots(
            @Valid @RequestBody GenerateSlotsRequest request,
            @AuthenticationPrincipal AuthenticatedAccount actor,
            HttpServletRequest servletRequest) {
        return ApiResponse.ok(adminManagementService.generateSlots(
                request, actor, AuditRequestMetadata.from(servletRequest)));
    }

    @GetMapping("/service-types")
    ApiResponse<List<ServiceTypeResponse>> listServiceTypes() {
        return ApiResponse.ok(adminManagementService.listServiceTypes());
    }
}
