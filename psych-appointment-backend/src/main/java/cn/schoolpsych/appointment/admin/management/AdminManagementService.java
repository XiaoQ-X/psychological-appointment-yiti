package cn.schoolpsych.appointment.admin.management;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.schoolpsych.appointment.admin.audit.AuditActions;
import cn.schoolpsych.appointment.admin.audit.AuditLogService;
import cn.schoolpsych.appointment.admin.audit.AuditRequestMetadata;
import cn.schoolpsych.appointment.domain.account.Account;
import cn.schoolpsych.appointment.domain.account.AccountRole;
import cn.schoolpsych.appointment.domain.audit.SensitiveLevel;
import cn.schoolpsych.appointment.domain.counselor.Counselor;
import cn.schoolpsych.appointment.domain.location.Campus;
import cn.schoolpsych.appointment.domain.location.Room;
import cn.schoolpsych.appointment.domain.rule.AppointmentRuleSet;
import cn.schoolpsych.appointment.domain.schedule.AppointmentSlot;
import cn.schoolpsych.appointment.domain.schedule.CounselorScheduleTemplate;
import cn.schoolpsych.appointment.domain.service.ServiceType;
import cn.schoolpsych.appointment.repository.AccountRepository;
import cn.schoolpsych.appointment.repository.AppointmentRuleSetRepository;
import cn.schoolpsych.appointment.repository.AppointmentSlotRepository;
import cn.schoolpsych.appointment.repository.CampusRepository;
import cn.schoolpsych.appointment.repository.CounselorRepository;
import cn.schoolpsych.appointment.repository.CounselorScheduleTemplateRepository;
import cn.schoolpsych.appointment.repository.RoomRepository;
import cn.schoolpsych.appointment.repository.ServiceTypeRepository;
import cn.schoolpsych.appointment.security.AuthenticatedAccount;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminManagementService {

    private static final int DEFAULT_SLOT_GAP_MINUTES = 10;
    private static final int MAX_GENERATION_DAYS = 31;

    private final CampusRepository campusRepository;
    private final RoomRepository roomRepository;
    private final CounselorRepository counselorRepository;
    private final AccountRepository accountRepository;
    private final ServiceTypeRepository serviceTypeRepository;
    private final CounselorScheduleTemplateRepository scheduleTemplateRepository;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRuleSetRepository ruleSetRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;

    public AdminManagementService(
            CampusRepository campusRepository,
            RoomRepository roomRepository,
            CounselorRepository counselorRepository,
            AccountRepository accountRepository,
            ServiceTypeRepository serviceTypeRepository,
            CounselorScheduleTemplateRepository scheduleTemplateRepository,
            AppointmentSlotRepository slotRepository,
            AppointmentRuleSetRepository ruleSetRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            AuditLogService auditLogService) {
        this.campusRepository = campusRepository;
        this.roomRepository = roomRepository;
        this.counselorRepository = counselorRepository;
        this.accountRepository = accountRepository;
        this.serviceTypeRepository = serviceTypeRepository;
        this.scheduleTemplateRepository = scheduleTemplateRepository;
        this.slotRepository = slotRepository;
        this.ruleSetRepository = ruleSetRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CampusResponse createCampus(
            CampusRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        Campus campus = campusRepository.save(Campus.create(
                request.name().trim(),
                blankToNull(request.address()),
                normalizeStatus(request.status())));
        CampusResponse response = CampusResponse.from(campus);
        auditLogService.record(actor, AuditActions.CAMPUS_CREATED, "CAMPUS", campus.getId(),
                SensitiveLevel.NORMAL, metadata, Map.of("status", campus.getStatus()));
        return response;
    }

    @Transactional
    public CampusResponse updateCampus(
            Long id,
            CampusRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        Campus campus = campusRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Campus not found"));
        campus.update(request.name().trim(), blankToNull(request.address()), normalizeStatus(request.status()));
        CampusResponse response = CampusResponse.from(campusRepository.save(campus));
        auditLogService.record(actor, AuditActions.CAMPUS_UPDATED, "CAMPUS", campus.getId(),
                SensitiveLevel.NORMAL, metadata, Map.of("status", campus.getStatus()));
        return response;
    }

    @Transactional(readOnly = true)
    public List<CampusResponse> listCampuses(String status) {
        return filterByStatus(campusRepository.findAll(Sort.by(Sort.Direction.ASC, "id")), status).stream()
                .map(CampusResponse::from)
                .toList();
    }

    @Transactional
    public RoomResponse createRoom(
            RoomRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        Campus campus = requireCampus(request.campusId());
        Room room = roomRepository.save(Room.create(
                campus.getId(),
                request.name().trim(),
                request.locationDesc().trim(),
                normalizeStatus(request.status())));
        RoomResponse response = RoomResponse.from(room, campus.getName());
        auditLogService.record(actor, AuditActions.ROOM_CREATED, "ROOM", room.getId(),
                SensitiveLevel.NORMAL, metadata, Map.of("campusId", campus.getId(), "status", room.getStatus()));
        return response;
    }

    @Transactional
    public RoomResponse updateRoom(
            Long id,
            RoomRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        Campus campus = requireCampus(request.campusId());
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
        room.update(campus.getId(), request.name().trim(), request.locationDesc().trim(), normalizeStatus(request.status()));
        RoomResponse response = RoomResponse.from(roomRepository.save(room), campus.getName());
        auditLogService.record(actor, AuditActions.ROOM_UPDATED, "ROOM", room.getId(),
                SensitiveLevel.NORMAL, metadata, Map.of("campusId", campus.getId(), "status", room.getStatus()));
        return response;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms(Long campusId, String status) {
        Map<Long, Campus> campuses = mapById(campusRepository.findAll());
        return roomRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(room -> campusId == null || room.getCampusId().equals(campusId))
                .filter(room -> isStatusMatch(room.getStatus(), status))
                .map(room -> RoomResponse.from(room, nameOf(campuses.get(room.getCampusId()))))
                .toList();
    }

    @Transactional
    public CounselorResponse createCounselor(
            CounselorRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        if (accountRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (request.campusId() != null) {
            requireCampus(request.campusId());
        }
        Account account = accountRepository.save(Account.create(
                request.username().trim(),
                passwordEncoder.encode(request.initialPassword()),
                AccountRole.COUNSELOR,
                true));
        Counselor counselor = counselorRepository.save(Counselor.create(
                account.getId(),
                request.name().trim(),
                blankToNull(request.avatarUrl()),
                blankToNull(request.title()),
                blankToNull(request.gender()),
                request.campusId(),
                toJsonList(request.expertise()),
                blankToNull(request.intro()),
                blankToNull(request.trainingBackground()),
                toJsonList(request.serviceModes()),
                valueOrZero(request.maxDailyCount()),
                valueOrTrue(request.visible()),
                normalizeStatus(request.status())));
        CounselorResponse response = counselorResponse(counselor);
        auditLogService.record(actor, AuditActions.COUNSELOR_CREATED, "COUNSELOR", counselor.getId(),
                SensitiveLevel.SENSITIVE, metadata,
                Map.of("accountId", account.getId(), "visible", counselor.isVisible(), "status", counselor.getStatus()));
        return response;
    }

    @Transactional
    public CounselorResponse updateCounselor(
            Long id,
            UpdateCounselorRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        Counselor counselor = counselorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Counselor not found"));
        if (request.campusId() != null) {
            requireCampus(request.campusId());
        }
        counselor.update(
                request.name().trim(),
                blankToNull(request.avatarUrl()),
                blankToNull(request.title()),
                blankToNull(request.gender()),
                request.campusId(),
                toJsonList(request.expertise()),
                blankToNull(request.intro()),
                blankToNull(request.trainingBackground()),
                toJsonList(request.serviceModes()),
                valueOrZero(request.maxDailyCount()),
                valueOrTrue(request.visible()),
                normalizeStatus(request.status()));
        CounselorResponse response = counselorResponse(counselorRepository.save(counselor));
        auditLogService.record(actor, AuditActions.COUNSELOR_UPDATED, "COUNSELOR", counselor.getId(),
                SensitiveLevel.SENSITIVE, metadata,
                Map.of("visible", counselor.isVisible(), "status", counselor.getStatus()));
        return response;
    }

    @Transactional(readOnly = true)
    public List<CounselorResponse> listCounselors(String status) {
        List<Counselor> counselors = counselorRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(counselor -> isStatusMatch(counselor.getStatus(), status))
                .toList();
        Map<Long, String> usernames = usernamesByAccountId(counselors.stream().map(Counselor::getAccountId).toList());
        Map<Long, Campus> campuses = mapById(campusRepository.findAll());
        return counselors.stream()
                .map(counselor -> counselorResponse(counselor, usernames, campuses))
                .toList();
    }

    @Transactional(readOnly = true)
    public CounselorResponse getCounselor(Long id) {
        Counselor counselor = counselorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Counselor not found"));
        return counselorResponse(counselor);
    }

    @Transactional
    public ScheduleTemplateResponse createScheduleTemplate(
            ScheduleTemplateRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        validateScheduleRequest(request);
        CounselorScheduleTemplate template = scheduleTemplateRepository.save(CounselorScheduleTemplate.create(
                request.counselorId(),
                request.campusId(),
                request.roomId(),
                request.serviceTypeId(),
                request.dayOfWeek().byteValue(),
                request.startTime(),
                request.endTime(),
                request.effectiveFrom(),
                request.effectiveTo(),
                normalizeStatus(request.status())));
        ScheduleTemplateResponse response = scheduleTemplateResponse(template);
        auditLogService.record(actor, AuditActions.SCHEDULE_CREATED, "SCHEDULE_TEMPLATE", template.getId(),
                SensitiveLevel.NORMAL, metadata,
                Map.of("counselorId", template.getCounselorId(), "dayOfWeek", template.getDayOfWeek()));
        return response;
    }

    @Transactional
    public ScheduleTemplateResponse updateScheduleTemplate(
            Long id,
            ScheduleTemplateRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        validateScheduleRequest(request);
        CounselorScheduleTemplate template = scheduleTemplateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule template not found"));
        template.update(
                request.counselorId(),
                request.campusId(),
                request.roomId(),
                request.serviceTypeId(),
                request.dayOfWeek().byteValue(),
                request.startTime(),
                request.endTime(),
                request.effectiveFrom(),
                request.effectiveTo(),
                normalizeStatus(request.status()));
        ScheduleTemplateResponse response = scheduleTemplateResponse(scheduleTemplateRepository.save(template));
        auditLogService.record(actor, AuditActions.SCHEDULE_UPDATED, "SCHEDULE_TEMPLATE", template.getId(),
                SensitiveLevel.NORMAL, metadata,
                Map.of("counselorId", template.getCounselorId(), "dayOfWeek", template.getDayOfWeek()));
        return response;
    }

    @Transactional(readOnly = true)
    public List<ScheduleTemplateResponse> listScheduleTemplates(Long counselorId, String status) {
        List<CounselorScheduleTemplate> templates = scheduleTemplateRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .filter(template -> counselorId == null || template.getCounselorId().equals(counselorId))
                .filter(template -> isStatusMatch(template.getStatus(), status))
                .toList();
        return scheduleTemplateResponses(templates);
    }

    @Transactional(readOnly = true)
    public List<ServiceTypeResponse> listServiceTypes() {
        return serviceTypeRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(ServiceTypeResponse::from)
                .toList();
    }

    @Transactional
    public GenerateSlotsResponse generateSlots(
            GenerateSlotsRequest request,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        validateGenerationRange(request.startDate(), request.endDate());
        if (request.counselorId() != null) {
            requireCounselor(request.counselorId());
        }

        Set<Byte> dayOfWeeks = dayOfWeeksInRange(request.startDate(), request.endDate());
        List<CounselorScheduleTemplate> templates = scheduleTemplateRepository.findActiveForGeneration(
                request.startDate(),
                request.endDate(),
                dayOfWeeks,
                request.counselorId());
        if (templates.isEmpty()) {
            GenerateSlotsResponse response = new GenerateSlotsResponse(
                    request.startDate(), request.endDate(), request.counselorId(), 0, 0, 0, 0, 0);
            auditSlotGeneration(request, response, actor, metadata);
            return response;
        }

        Map<Long, ServiceType> serviceTypes = mapById(serviceTypeRepository.findAllById(
                templates.stream().map(CounselorScheduleTemplate::getServiceTypeId).collect(Collectors.toSet())));
        Map<Long, Counselor> counselors = mapById(counselorRepository.findAllById(
                templates.stream().map(CounselorScheduleTemplate::getCounselorId).collect(Collectors.toSet())));
        Map<DailyKey, Long> dailyCounts = new HashMap<>();
        List<AppointmentSlot> slotsToSave = new ArrayList<>();
        int generatedCount = 0;
        int existingCount = 0;
        int skippedPastCount = 0;
        int skippedDisabledServiceCount = 0;
        int skippedLimitCount = 0;
        int slotGapMinutes = activeSlotGapMinutes();
        LocalDateTime now = LocalDateTime.now();

        for (LocalDate date = request.startDate(); !date.isAfter(request.endDate()); date = date.plusDays(1)) {
            byte dayOfWeek = (byte) date.getDayOfWeek().getValue();
            for (CounselorScheduleTemplate template : templates) {
                if (template.getDayOfWeek() != dayOfWeek || !template.appliesTo(date)) {
                    continue;
                }
                ServiceType serviceType = serviceTypes.get(template.getServiceTypeId());
                if (serviceType == null || !serviceType.isEnabled() || serviceType.getDurationMinutes() <= 0) {
                    skippedDisabledServiceCount++;
                    continue;
                }
                Counselor counselor = counselors.get(template.getCounselorId());
                if (counselor == null || !"ACTIVE".equals(counselor.getStatus())) {
                    skippedDisabledServiceCount++;
                    continue;
                }
                LocalDateTime cursor = LocalDateTime.of(date, template.getStartTime());
                LocalDateTime boundary = LocalDateTime.of(date, template.getEndTime());
                while (!cursor.plusMinutes(serviceType.getDurationMinutes()).isAfter(boundary)) {
                    LocalDateTime slotStart = cursor;
                    LocalDateTime slotEnd = cursor.plusMinutes(serviceType.getDurationMinutes());
                    if (!slotStart.isAfter(now)) {
                        skippedPastCount++;
                    } else if (slotRepository.existsByCounselorIdAndStartAtAndEndAt(template.getCounselorId(), slotStart, slotEnd)) {
                        existingCount++;
                    } else if (isDailyLimitReached(counselor, date, dailyCounts)) {
                        skippedLimitCount++;
                    } else {
                        slotsToSave.add(AppointmentSlot.available(
                                template.getCounselorId(),
                                template.getCampusId(),
                                template.getRoomId(),
                                template.getServiceTypeId(),
                                slotStart,
                                slotEnd));
                        incrementDailyCount(template.getCounselorId(), date, dailyCounts);
                        generatedCount++;
                    }
                    cursor = slotEnd.plusMinutes(slotGapMinutes);
                }
            }
        }
        slotRepository.saveAll(slotsToSave);
        GenerateSlotsResponse response = new GenerateSlotsResponse(
                request.startDate(),
                request.endDate(),
                request.counselorId(),
                generatedCount,
                existingCount,
                skippedPastCount,
                skippedDisabledServiceCount,
                skippedLimitCount);
        auditSlotGeneration(request, response, actor, metadata);
        return response;
    }

    private void auditSlotGeneration(
            GenerateSlotsRequest request,
            GenerateSlotsResponse response,
            AuthenticatedAccount actor,
            AuditRequestMetadata metadata) {
        auditLogService.record(
                actor,
                AuditActions.SLOTS_GENERATED,
                "APPOINTMENT_SLOT",
                null,
                SensitiveLevel.NORMAL,
                metadata,
                Map.of(
                        "startDate", request.startDate(),
                        "endDate", request.endDate(),
                        "scope", request.counselorId() == null ? "ALL_COUNSELORS" : request.counselorId(),
                        "generatedCount", response.generatedCount(),
                        "existingCount", response.existingCount()));
    }

    private void validateScheduleRequest(ScheduleTemplateRequest request) {
        if (!request.endTime().isAfter(request.startTime())) {
            throw new IllegalArgumentException("Schedule endTime must be after startTime");
        }
        if (request.effectiveTo() != null && request.effectiveTo().isBefore(request.effectiveFrom())) {
            throw new IllegalArgumentException("Schedule effectiveTo must not be before effectiveFrom");
        }
        requireCounselor(request.counselorId());
        requireCampus(request.campusId());
        if (request.roomId() != null) {
            Room room = requireRoom(request.roomId());
            if (!room.getCampusId().equals(request.campusId())) {
                throw new IllegalArgumentException("Room does not belong to campus");
            }
        }
        ServiceType serviceType = requireServiceType(request.serviceTypeId());
        if (!serviceType.isEnabled()) {
            throw new IllegalArgumentException("Service type is disabled");
        }
    }

    private void validateGenerationRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_GENERATION_DAYS) {
            throw new IllegalArgumentException("Slot generation range cannot exceed " + MAX_GENERATION_DAYS + " days");
        }
    }

    private int activeSlotGapMinutes() {
        return ruleSetRepository.findFirstByActiveTrueOrderByEffectiveFromDesc()
                .map(AppointmentRuleSet::getSettingsJson)
                .map(this::slotGapMinutesFromJson)
                .orElse(DEFAULT_SLOT_GAP_MINUTES);
    }

    private int slotGapMinutesFromJson(String settingsJson) {
        try {
            JsonNode node = objectMapper.readTree(settingsJson);
            return node.path("slotGapMinutes").asInt(DEFAULT_SLOT_GAP_MINUTES);
        } catch (JsonProcessingException exception) {
            return DEFAULT_SLOT_GAP_MINUTES;
        }
    }

    private boolean isDailyLimitReached(Counselor counselor, LocalDate date, Map<DailyKey, Long> dailyCounts) {
        int maxDailyCount = counselor.getMaxDailyCount();
        if (maxDailyCount <= 0) {
            return false;
        }
        long currentCount = dailyCounts.computeIfAbsent(
                new DailyKey(counselor.getId(), date),
                key -> slotRepository.countByCounselorIdAndStartAtGreaterThanEqualAndStartAtLessThan(
                        key.counselorId(),
                        key.date().atStartOfDay(),
                        key.date().plusDays(1).atStartOfDay()));
        return currentCount >= maxDailyCount;
    }

    private void incrementDailyCount(Long counselorId, LocalDate date, Map<DailyKey, Long> dailyCounts) {
        dailyCounts.merge(new DailyKey(counselorId, date), 1L, Long::sum);
    }

    private Set<Byte> dayOfWeeksInRange(LocalDate startDate, LocalDate endDate) {
        Set<Byte> dayOfWeeks = new HashSet<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dayOfWeeks.add((byte) date.getDayOfWeek().getValue());
        }
        return dayOfWeeks;
    }

    private CounselorResponse counselorResponse(Counselor counselor) {
        Map<Long, String> usernames = usernamesByAccountId(List.of(counselor.getAccountId()));
        Map<Long, Campus> campuses = mapById(counselor.getCampusId() == null
                ? List.of()
                : campusRepository.findAllById(List.of(counselor.getCampusId())));
        return counselorResponse(counselor, usernames, campuses);
    }

    private CounselorResponse counselorResponse(Counselor counselor, Map<Long, String> usernames, Map<Long, Campus> campuses) {
        return CounselorResponse.from(
                counselor,
                usernames.get(counselor.getAccountId()),
                counselor.getCampusId() == null ? null : nameOf(campuses.get(counselor.getCampusId())),
                readJsonList(counselor.getExpertiseJson()),
                readJsonList(counselor.getServiceModesJson()));
    }

    private List<ScheduleTemplateResponse> scheduleTemplateResponses(Collection<CounselorScheduleTemplate> templates) {
        Map<Long, Counselor> counselors = mapById(counselorRepository.findAllById(
                templates.stream().map(CounselorScheduleTemplate::getCounselorId).collect(Collectors.toSet())));
        Map<Long, Campus> campuses = mapById(campusRepository.findAllById(
                templates.stream().map(CounselorScheduleTemplate::getCampusId).collect(Collectors.toSet())));
        Map<Long, Room> rooms = mapById(roomRepository.findAllById(
                templates.stream().map(CounselorScheduleTemplate::getRoomId).filter(id -> id != null).collect(Collectors.toSet())));
        Map<Long, ServiceType> serviceTypes = mapById(serviceTypeRepository.findAllById(
                templates.stream().map(CounselorScheduleTemplate::getServiceTypeId).collect(Collectors.toSet())));
        return templates.stream()
                .map(template -> ScheduleTemplateResponse.from(
                        template,
                        nameOf(counselors.get(template.getCounselorId())),
                        nameOf(campuses.get(template.getCampusId())),
                        template.getRoomId() == null ? null : nameOf(rooms.get(template.getRoomId())),
                        nameOf(serviceTypes.get(template.getServiceTypeId()))))
                .toList();
    }

    private ScheduleTemplateResponse scheduleTemplateResponse(CounselorScheduleTemplate template) {
        return scheduleTemplateResponses(List.of(template)).get(0);
    }

    private Campus requireCampus(Long campusId) {
        return campusRepository.findById(campusId)
                .orElseThrow(() -> new IllegalArgumentException("Campus not found"));
    }

    private Room requireRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
    }

    private Counselor requireCounselor(Long counselorId) {
        return counselorRepository.findById(counselorId)
                .orElseThrow(() -> new IllegalArgumentException("Counselor not found"));
    }

    private ServiceType requireServiceType(Long serviceTypeId) {
        return serviceTypeRepository.findById(serviceTypeId)
                .orElseThrow(() -> new IllegalArgumentException("Service type not found"));
    }

    private List<Campus> filterByStatus(List<Campus> campuses, String status) {
        return campuses.stream()
                .filter(campus -> isStatusMatch(campus.getStatus(), status))
                .toList();
    }

    private boolean isStatusMatch(String value, String requestedStatus) {
        return isBlank(requestedStatus) || value.equals(normalizeStatus(requestedStatus));
    }

    private String normalizeStatus(String status) {
        if (isBlank(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!normalized.equals("ACTIVE") && !normalized.equals("DISABLED")) {
            throw new IllegalArgumentException("Unsupported status: " + status);
        }
        return normalized;
    }

    private Map<Long, String> usernamesByAccountId(Collection<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Map.of();
        }
        return accountRepository.findAllById(accountIds).stream()
                .collect(Collectors.toMap(Account::getId, Account::getUsername));
    }

    private <T extends cn.schoolpsych.appointment.domain.common.BaseEntity> Map<Long, T> mapById(Iterable<T> entities) {
        Map<Long, T> result = new LinkedHashMap<>();
        for (T entity : entities) {
            result.put(entity.getId(), entity);
        }
        return result;
    }

    private String nameOf(Campus campus) {
        return campus == null ? null : campus.getName();
    }

    private String nameOf(Room room) {
        return room == null ? null : room.getName();
    }

    private String nameOf(Counselor counselor) {
        return counselor == null ? null : counselor.getName();
    }

    private String nameOf(ServiceType serviceType) {
        return serviceType == null ? null : serviceType.getName();
    }

    private String toJsonList(List<String> values) {
        try {
            return objectMapper.writeValueAsString(sanitizeList(values));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid list value", exception);
        }
    }

    private List<String> readJsonList(String json) {
        if (isBlank(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean valueOrTrue(Boolean value) {
        return value == null || value;
    }

    private record DailyKey(Long counselorId, LocalDate date) {
    }
}
