package cn.schoolpsych.appointment.domain.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class StudentBookingEligibilityTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 14, 12, 0);

    @Test
    void allowsThresholdMinusOneAndBlocksAtThreshold() {
        Student student = student();
        student.recordNoShow();

        assertThat(student.canBookNow(NOW, 2)).isTrue();

        student.recordNoShow();

        assertThat(student.canBookNow(NOW, 2)).isFalse();
    }

    @Test
    void activeRuleChangesTakeEffectWithoutRewritingStudentState() {
        Student student = student();
        student.recordNoShow();
        student.recordNoShow();

        assertThat(student.canBookNow(NOW, 3)).isTrue();
        assertThat(student.canBookNow(NOW, 2)).isFalse();
    }

    @Test
    void preservesStatusAndTemporaryRestrictionChecks() {
        Student student = student();
        ReflectionTestUtils.setField(student, "bookingRestrictedUntil", NOW.plusDays(1));
        assertThat(student.canBookNow(NOW, 2)).isFalse();
        assertThat(student.canBookNow(NOW.plusDays(2), 2)).isTrue();

        ReflectionTestUtils.setField(student, "status", "DISABLED");
        assertThat(student.canBookNow(NOW.plusDays(2), 2)).isFalse();
    }

    @Test
    void rejectsInvalidThresholdInsteadOfSilentlyDisablingRestriction() {
        assertThatThrownBy(() -> student().canBookNow(NOW, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("threshold");
    }

    private Student student() {
        return Student.create(
                1L,
                "20260001",
                "Test Student",
                null,
                "Test College",
                null,
                null,
                null,
                null,
                "ACTIVE");
    }
}
