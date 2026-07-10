package cn.schoolpsych.appointment.admin.audit;

public final class AuditActions {

    public static final String ADMIN_LOGIN = "ADMIN_LOGIN";
    public static final String STUDENT_IMPORT = "STUDENT_IMPORT";
    public static final String CAMPUS_CREATED = "CAMPUS_CREATED";
    public static final String CAMPUS_UPDATED = "CAMPUS_UPDATED";
    public static final String ROOM_CREATED = "ROOM_CREATED";
    public static final String ROOM_UPDATED = "ROOM_UPDATED";
    public static final String COUNSELOR_CREATED = "COUNSELOR_CREATED";
    public static final String COUNSELOR_UPDATED = "COUNSELOR_UPDATED";
    public static final String SCHEDULE_CREATED = "SCHEDULE_CREATED";
    public static final String SCHEDULE_UPDATED = "SCHEDULE_UPDATED";
    public static final String SLOTS_GENERATED = "SLOTS_GENERATED";
    public static final String APPOINTMENT_CANCELED = "APPOINTMENT_CANCELED";
    public static final String RISK_REVIEWED = "RISK_REVIEWED";
    public static final String APPOINTMENT_RULE_CREATED = "APPOINTMENT_RULE_CREATED";
    public static final String APPOINTMENT_RULE_UPDATED = "APPOINTMENT_RULE_UPDATED";
    public static final String APPOINTMENT_RULE_ACTIVATED = "APPOINTMENT_RULE_ACTIVATED";

    private AuditActions() {
    }
}
