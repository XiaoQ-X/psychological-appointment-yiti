package cn.schoolpsych.appointment.student.availability;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.domain.common.BaseEntity;
import cn.schoolpsych.appointment.domain.counselor.Counselor;
import cn.schoolpsych.appointment.domain.location.Campus;
import cn.schoolpsych.appointment.domain.location.Room;
import cn.schoolpsych.appointment.domain.schedule.AppointmentSlot;
import cn.schoolpsych.appointment.domain.schedule.SlotStatus;
import cn.schoolpsych.appointment.domain.service.ServiceType;
import cn.schoolpsych.appointment.repository.AppointmentSlotRepository;
import cn.schoolpsych.appointment.repository.AppointmentRuleSetRepository;
import cn.schoolpsych.appointment.repository.CampusRepository;
import cn.schoolpsych.appointment.repository.CounselorRepository;
import cn.schoolpsych.appointment.repository.RoomRepository;
import cn.schoolpsych.appointment.repository.ServiceTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentAvailabilityService {

    private static final int DEFAULT_LOOKAHEAD_DAYS = 14;
    private static final int DEFAULT_MIN_BOOKING_HOURS_AHEAD = 24;
    private static final int MAX_QUERY_DAYS = 61;

    private final CounselorRepository counselorRepository;
    private final AppointmentSlotRepository slotRepository;
    private final CampusRepository campusRepository;
    private final RoomRepository roomRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final AppointmentRuleSetRepository ruleSetRepository;
    private final ObjectMapper objectMapper;

    public StudentAvailabilityService(
            CounselorRepository counselorRepository,
            AppointmentSlotRepository slotRepository,
            CampusRepository campusRepository,
            RoomRepository roomRepository,
            ServiceTypeRepository serviceTypeRepository,
            AppointmentRuleSetRepository ruleSetRepository,
            ObjectMapper objectMapper) {
        this.counselorRepository = counselorRepository;
        this.slotRepository = slotRepository;
        this.campusRepository = campusRepository;
        this.roomRepository = roomRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.ruleSetRepository = ruleSetRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public List<StudentCounselorResponse> listCounselors(Long campusId, LocalDate from, LocalDate to) {
        slotRepository.releaseExpiredLocks(LocalDateTime.now());
        DateRange range = normalizeRange(from, to);
        List<Counselor> counselors = campusId == null
                ? counselorRepository.findByStatusAndVisibleTrueOrderByIdAsc("ACTIVE")
                : counselorRepository.findByCampusIdAndStatusAndVisibleTrueOrderByIdAsc(campusId, "ACTIVE");
        if (counselors.isEmpty()) {
            return List.of();
        }

        Map<Long, Campus> campuses = mapById(campusRepository.findAllById(
                counselors.stream().map(Counselor::getCampusId).filter(id -> id != null).collect(Collectors.toSet())));
        SlotSummary slotSummary = summarizeSlots(
                counselors.stream().map(Counselor::getId).toList(),
                range);

        return counselors.stream()
                .map(counselor -> new StudentCounselorResponse(
                        counselor.getId(),
                        counselor.getName(),
                        counselor.getAvatarUrl(),
                        counselor.getTitle(),
                        counselor.getGender(),
                        counselor.getCampusId(),
                        counselor.getCampusId() == null ? null : nameOf(campuses.get(counselor.getCampusId())),
                        readJsonList(counselor.getExpertiseJson()),
                        readJsonList(counselor.getServiceModesJson()),
                        slotSummary.nextAvailableAtByCounselor().get(counselor.getId()),
                        slotSummary.countByCounselor().getOrDefault(counselor.getId(), 0L)))
                .toList();
    }

    @Transactional
    public StudentCounselorDetailResponse getCounselor(Long counselorId, LocalDate from, LocalDate to) {
        slotRepository.releaseExpiredLocks(LocalDateTime.now());
        DateRange range = normalizeRange(from, to);
        Counselor counselor = requireVisibleCounselor(counselorId);
        Campus campus = counselor.getCampusId() == null
                ? null
                : campusRepository.findById(counselor.getCampusId()).orElse(null);
        SlotSummary slotSummary = summarizeSlots(List.of(counselorId), range);
        return new StudentCounselorDetailResponse(
                counselor.getId(),
                counselor.getName(),
                counselor.getAvatarUrl(),
                counselor.getTitle(),
                counselor.getGender(),
                counselor.getCampusId(),
                nameOf(campus),
                readJsonList(counselor.getExpertiseJson()),
                counselor.getIntro(),
                counselor.getTrainingBackground(),
                readJsonList(counselor.getServiceModesJson()),
                slotSummary.nextAvailableAtByCounselor().get(counselorId),
                slotSummary.countByCounselor().getOrDefault(counselorId, 0L));
    }

    @Transactional
    public List<StudentSlotResponse> listSlots(Long counselorId, LocalDate from, LocalDate to) {
        slotRepository.releaseExpiredLocks(LocalDateTime.now());
        Counselor counselor = requireVisibleCounselor(counselorId);
        DateRange range = normalizeRange(from, to);
        List<AppointmentSlot> slots = slotRepository
                .findByCounselorIdAndStatusAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                        counselorId,
                        SlotStatus.AVAILABLE,
                        range.startAt(),
                        range.endExclusive())
                .stream()
                .filter(slot -> slot.getStartAt().isAfter(LocalDateTime.now()))
                .toList();
        Map<Long, Campus> campuses = mapById(campusRepository.findAllById(
                slots.stream().map(AppointmentSlot::getCampusId).collect(Collectors.toSet())));
        Map<Long, Room> rooms = mapById(roomRepository.findAllById(
                slots.stream().map(AppointmentSlot::getRoomId).filter(id -> id != null).collect(Collectors.toSet())));
        Map<Long, ServiceType> serviceTypes = mapById(serviceTypeRepository.findAllById(
                slots.stream().map(AppointmentSlot::getServiceTypeId).collect(Collectors.toSet())));
        return slots.stream()
                .map(slot -> {
                    ServiceType serviceType = serviceTypes.get(slot.getServiceTypeId());
                    return new StudentSlotResponse(
                            slot.getId(),
                            slot.getCounselorId(),
                            counselor.getName(),
                            slot.getCampusId(),
                            nameOf(campuses.get(slot.getCampusId())),
                            slot.getRoomId(),
                            slot.getRoomId() == null ? null : nameOf(rooms.get(slot.getRoomId())),
                            slot.getServiceTypeId(),
                            nameOf(serviceType),
                            serviceType == null ? 0 : serviceType.getDurationMinutes(),
                            slot.getStartAt(),
                            slot.getEndAt(),
                            slot.getStatus());
                })
                .toList();
    }

    private SlotSummary summarizeSlots(Collection<Long> counselorIds, DateRange range) {
        if (counselorIds.isEmpty()) {
            return new SlotSummary(Map.of(), Map.of());
        }
        List<AppointmentSlot> slots = slotRepository
                .findByCounselorIdInAndStatusAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                        counselorIds,
                        SlotStatus.AVAILABLE,
                        range.startAt(),
                        range.endExclusive())
                .stream()
                .filter(slot -> slot.getStartAt().isAfter(LocalDateTime.now()))
                .toList();
        Map<Long, Long> countByCounselor = slots.stream()
                .collect(Collectors.groupingBy(AppointmentSlot::getCounselorId, Collectors.counting()));
        Map<Long, LocalDateTime> nextAvailableAtByCounselor = new HashMap<>();
        slots.stream()
                .sorted(Comparator.comparing(AppointmentSlot::getStartAt))
                .forEach(slot -> nextAvailableAtByCounselor.putIfAbsent(slot.getCounselorId(), slot.getStartAt()));
        return new SlotSummary(countByCounselor, nextAvailableAtByCounselor);
    }

    private Counselor requireVisibleCounselor(Long counselorId) {
        Counselor counselor = counselorRepository.findById(counselorId)
                .orElseThrow(() -> new IllegalArgumentException("Counselor not found"));
        if (!"ACTIVE".equals(counselor.getStatus()) || !counselor.isVisible()) {
            throw new IllegalArgumentException("Counselor is not available");
        }
        return counselor;
    }

    private DateRange normalizeRange(LocalDate from, LocalDate to) {
        LocalDateTime now = LocalDateTime.now();
        JsonNode settings = activeRuleSettings();
        int maxBookingDaysAhead = settings.path("maxBookingDaysAhead").asInt(DEFAULT_LOOKAHEAD_DAYS);
        int minBookingHoursAhead = settings.path("minBookingHoursAhead").asInt(DEFAULT_MIN_BOOKING_HOURS_AHEAD);
        LocalDate start = from == null ? now.toLocalDate() : from;
        LocalDate end = to == null ? now.toLocalDate().plusDays(maxBookingDaysAhead) : to;
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("to must not be before from");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_QUERY_DAYS) {
            throw new IllegalArgumentException("Availability query range cannot exceed " + MAX_QUERY_DAYS + " days");
        }
        LocalDateTime requestedStart = start.atStartOfDay();
        LocalDateTime requestedEnd = end.plusDays(1).atStartOfDay();
        LocalDateTime policyStart = now.plusHours(minBookingHoursAhead);
        LocalDateTime policyEnd = now.plusDays(maxBookingDaysAhead).plusSeconds(1);
        return new DateRange(
                requestedStart.isAfter(policyStart) ? requestedStart : policyStart,
                requestedEnd.isBefore(policyEnd) ? requestedEnd : policyEnd);
    }

    private JsonNode activeRuleSettings() {
        return ruleSetRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()
                .map(ruleSet -> {
                    try {
                        return objectMapper.readTree(ruleSet.getSettingsJson());
                    } catch (JsonProcessingException exception) {
                        return objectMapper.createObjectNode();
                    }
                })
                .orElseGet(objectMapper::createObjectNode);
    }

    private <T extends BaseEntity> Map<Long, T> mapById(Iterable<T> entities) {
        Map<Long, T> result = new HashMap<>();
        for (T entity : entities) {
            result.put(entity.getId(), entity);
        }
        return result;
    }

    private List<String> readJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String nameOf(Campus campus) {
        return campus == null ? null : campus.getName();
    }

    private String nameOf(Room room) {
        return room == null ? null : room.getName();
    }

    private String nameOf(ServiceType serviceType) {
        return serviceType == null ? null : serviceType.getName();
    }

    private record DateRange(LocalDateTime startAt, LocalDateTime endExclusive) {
    }

    private record SlotSummary(Map<Long, Long> countByCounselor, Map<Long, LocalDateTime> nextAvailableAtByCounselor) {
    }
}
