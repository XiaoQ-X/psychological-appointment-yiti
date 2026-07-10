package cn.schoolpsych.appointment.student.appointment;

import java.util.List;

import cn.schoolpsych.appointment.domain.appointment.RiskLevel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitAppointmentRequest(
        @NotNull Long slotId,
        boolean firstVisit,
        @NotEmpty List<@NotBlank String> issueTypes,
        @NotBlank @Size(max = 300) String description,
        @Size(max = 200) String expectedHelp,
        @NotNull RiskLevel urgencyLevel,
        @Size(max = 255) String contactTime,
        boolean consentAgreed,
        @NotNull @Valid RiskScreeningRequest risk) {
}
