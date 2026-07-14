package cn.schoolpsych.appointment.domain.appointment;

import java.util.List;

public record AppointmentFormMetadata(
        List<String> issueTypes,
        RiskLevel urgencyLevel,
        String contactTime) {

    public AppointmentFormMetadata {
        issueTypes = issueTypes == null ? List.of() : List.copyOf(issueTypes);
    }
}
