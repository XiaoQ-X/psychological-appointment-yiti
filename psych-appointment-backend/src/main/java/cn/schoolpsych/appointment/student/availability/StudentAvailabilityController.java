package cn.schoolpsych.appointment.student.availability;

import java.time.LocalDate;
import java.util.List;

import cn.schoolpsych.appointment.common.api.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student/counselors")
@PreAuthorize("hasRole('STUDENT')")
public class StudentAvailabilityController {

    private final StudentAvailabilityService studentAvailabilityService;

    public StudentAvailabilityController(StudentAvailabilityService studentAvailabilityService) {
        this.studentAvailabilityService = studentAvailabilityService;
    }

    @GetMapping
    ApiResponse<List<StudentCounselorResponse>> listCounselors(
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(studentAvailabilityService.listCounselors(campusId, from, to));
    }

    @GetMapping("/{id}")
    ApiResponse<StudentCounselorDetailResponse> getCounselor(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(studentAvailabilityService.getCounselor(id, from, to));
    }

    @GetMapping("/{id}/slots")
    ApiResponse<List<StudentSlotResponse>> listSlots(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(studentAvailabilityService.listSlots(id, from, to));
    }
}
