package cn.schoolpsych.appointment.student.appointment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class AppointmentIssueTypeValidationTest {

    @Test
    void acceptsOnlySupportedIssueTypes() {
        assertThatCode(() -> StudentAppointmentService.ensureSupportedIssueTypes(
                List.of("academic-stress", "emotion", "sleep")))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> StudentAppointmentService.ensureSupportedIssueTypes(
                List.of("synthetic-private-category")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported appointment issue type");
    }
}
