package cn.schoolpsych.appointment.student.appointment;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.appointment.Appointment;
import cn.schoolpsych.appointment.repository.AppointmentRuleSetRepository;
import org.springframework.stereotype.Component;

@Component
public class StudentCancellationPolicy {

    private static final int DEFAULT_MIN_CANCEL_HOURS_AHEAD = 24;

    private final AppointmentRuleSetRepository ruleSetRepository;
    private final ObjectMapper objectMapper;

    public StudentCancellationPolicy(
            AppointmentRuleSetRepository ruleSetRepository,
            ObjectMapper objectMapper) {
        this.ruleSetRepository = ruleSetRepository;
        this.objectMapper = objectMapper;
    }

    public CancellationAvailability evaluate(Appointment appointment, LocalDateTime now) {
        int minCancelHoursAhead = minCancelHoursAhead();
        LocalDateTime cancelDeadline = appointment.getStartAt().minusHours(minCancelHoursAhead);
        boolean canCancel = appointment.canBeCanceledByStudent(now) && !cancelDeadline.isBefore(now);
        return new CancellationAvailability(canCancel, minCancelHoursAhead, cancelDeadline);
    }

    private int minCancelHoursAhead() {
        return ruleSetRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()
                .map(ruleSet -> readSettings(ruleSet.getSettingsJson()))
                .map(settings -> settings.path("minCancelHoursAhead").asInt(DEFAULT_MIN_CANCEL_HOURS_AHEAD))
                .orElse(DEFAULT_MIN_CANCEL_HOURS_AHEAD);
    }

    private JsonNode readSettings(String settingsJson) {
        try {
            return objectMapper.readTree(settingsJson);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    public record CancellationAvailability(
            boolean canCancel,
            int minCancelHoursAhead,
            LocalDateTime cancelDeadline) {
    }
}
