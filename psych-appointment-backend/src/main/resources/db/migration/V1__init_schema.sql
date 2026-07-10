create table accounts (
    id bigint primary key auto_increment,
    username varchar(64) not null,
    password_hash varchar(255) not null,
    role varchar(32) not null,
    status varchar(32) not null,
    wx_openid varchar(128) null,
    wx_unionid varchar(128) null,
    force_password_change boolean not null default true,
    login_fail_count int not null default 0,
    locked_until datetime(6) null,
    last_login_at datetime(6) null,
    password_changed_at datetime(6) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_accounts_username unique (username),
    constraint uk_accounts_wx_openid unique (wx_openid)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_accounts_role_status on accounts (role, status);

create table students (
    id bigint primary key auto_increment,
    account_id bigint not null,
    student_no varchar(64) not null,
    name varchar(64) not null,
    gender varchar(16) null,
    college varchar(128) not null,
    major varchar(128) null,
    class_name varchar(128) null,
    grade varchar(32) null,
    phone_encrypted blob null,
    status varchar(32) not null,
    no_show_count int not null default 0,
    booking_restricted_until datetime(6) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_students_account unique (account_id),
    constraint uk_students_student_no unique (student_no),
    constraint fk_students_account foreign key (account_id) references accounts (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_students_college_grade on students (college, grade);
create index idx_students_status on students (status);

create table student_import_batches (
    id bigint primary key auto_increment,
    file_name varchar(255) not null,
    total_count int not null default 0,
    success_count int not null default 0,
    failed_count int not null default 0,
    strategy varchar(32) not null,
    status varchar(32) not null,
    operator_id bigint not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table student_import_rows (
    id bigint primary key auto_increment,
    batch_id bigint not null,
    row_no int not null,
    student_no varchar(64) null,
    name varchar(64) null,
    status varchar(32) not null,
    error_message varchar(500) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint fk_student_import_rows_batch foreign key (batch_id) references student_import_batches (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table admins (
    id bigint primary key auto_increment,
    account_id bigint not null,
    name varchar(64) not null,
    department varchar(128) null,
    permission_scope_json json null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_admins_account unique (account_id),
    constraint fk_admins_account foreign key (account_id) references accounts (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table campuses (
    id bigint primary key auto_increment,
    name varchar(128) not null,
    address varchar(255) null,
    status varchar(32) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table counselors (
    id bigint primary key auto_increment,
    account_id bigint not null,
    name varchar(64) not null,
    avatar_url varchar(500) null,
    title varchar(128) null,
    gender varchar(16) null,
    campus_id bigint null,
    expertise_json json null,
    intro text null,
    training_background text null,
    service_modes_json json null,
    max_daily_count int not null default 0,
    is_visible boolean not null default true,
    status varchar(32) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_counselors_account unique (account_id),
    constraint fk_counselors_account foreign key (account_id) references accounts (id),
    constraint fk_counselors_campus foreign key (campus_id) references campuses (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_counselors_status_visible on counselors (status, is_visible);
create index idx_counselors_campus on counselors (campus_id);

create table rooms (
    id bigint primary key auto_increment,
    campus_id bigint not null,
    name varchar(128) not null,
    location_desc varchar(255) not null,
    status varchar(32) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint fk_rooms_campus foreign key (campus_id) references campuses (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_rooms_campus_status on rooms (campus_id, status);

create table service_types (
    id bigint primary key auto_increment,
    code varchar(64) not null,
    name varchar(128) not null,
    duration_minutes int not null,
    enabled boolean not null default true,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_service_types_code unique (code)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table school_terms (
    id bigint primary key auto_increment,
    name varchar(128) not null,
    start_date date not null,
    end_date date not null,
    is_current boolean not null default false,
    status varchar(32) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_school_terms_range on school_terms (start_date, end_date);
create index idx_school_terms_current_status on school_terms (is_current, status);

create table appointment_rule_sets (
    id bigint primary key auto_increment,
    name varchar(128) not null,
    settings_json json not null,
    is_active boolean not null default false,
    effective_from datetime(6) not null,
    effective_to datetime(6) null,
    published_by bigint not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table counselor_schedule_templates (
    id bigint primary key auto_increment,
    counselor_id bigint not null,
    campus_id bigint not null,
    room_id bigint null,
    service_type_id bigint not null,
    day_of_week tinyint not null,
    start_time time not null,
    end_time time not null,
    effective_from date not null,
    effective_to date null,
    status varchar(32) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint fk_schedule_templates_counselor foreign key (counselor_id) references counselors (id),
    constraint fk_schedule_templates_campus foreign key (campus_id) references campuses (id),
    constraint fk_schedule_templates_room foreign key (room_id) references rooms (id),
    constraint fk_schedule_templates_service_type foreign key (service_type_id) references service_types (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_schedule_templates_counselor_day_status on counselor_schedule_templates (counselor_id, day_of_week, status);

create table counselor_unavailable_periods (
    id bigint primary key auto_increment,
    counselor_id bigint not null,
    start_at datetime(6) not null,
    end_at datetime(6) not null,
    reason varchar(255) null,
    status varchar(32) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint fk_unavailable_counselor foreign key (counselor_id) references counselors (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_unavailable_counselor_time on counselor_unavailable_periods (counselor_id, start_at, end_at);

create table appointment_slots (
    id bigint primary key auto_increment,
    counselor_id bigint not null,
    campus_id bigint not null,
    room_id bigint null,
    service_type_id bigint not null,
    start_at datetime(6) not null,
    end_at datetime(6) not null,
    status varchar(32) not null,
    locked_by_student_id bigint null,
    locked_until datetime(6) null,
    appointment_id bigint null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_slots_counselor_time unique (counselor_id, start_at, end_at),
    constraint fk_slots_counselor foreign key (counselor_id) references counselors (id),
    constraint fk_slots_campus foreign key (campus_id) references campuses (id),
    constraint fk_slots_room foreign key (room_id) references rooms (id),
    constraint fk_slots_service_type foreign key (service_type_id) references service_types (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_slots_status_start on appointment_slots (status, start_at);
create index idx_slots_campus_start on appointment_slots (campus_id, start_at);

create table consent_versions (
    id bigint primary key auto_increment,
    version_no varchar(32) not null,
    title varchar(255) not null,
    content mediumtext not null,
    status varchar(32) not null,
    published_at datetime(6) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_consent_versions_no unique (version_no)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table consent_records (
    id bigint primary key auto_increment,
    student_id bigint not null,
    consent_version_id bigint not null,
    agreed_at datetime(6) not null,
    client_info varchar(255) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint fk_consent_records_student foreign key (student_id) references students (id),
    constraint fk_consent_records_version foreign key (consent_version_id) references consent_versions (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_consent_records_student_version on consent_records (student_id, consent_version_id);

create table appointments (
    id bigint primary key auto_increment,
    appointment_no varchar(64) not null,
    student_id bigint not null,
    counselor_id bigint not null,
    slot_id bigint not null,
    campus_id bigint not null,
    room_id bigint null,
    service_type_id bigint not null,
    semester_id bigint not null,
    start_at datetime(6) not null,
    end_at datetime(6) not null,
    rule_set_id bigint not null,
    consent_record_id bigint not null,
    status varchar(32) not null,
    risk_level varchar(32) not null,
    cancel_reason varchar(500) null,
    canceled_by bigint null,
    canceled_at datetime(6) null,
    checked_in_at datetime(6) null,
    completed_at datetime(6) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_appointments_no unique (appointment_no),
    constraint fk_appointments_student foreign key (student_id) references students (id),
    constraint fk_appointments_counselor foreign key (counselor_id) references counselors (id),
    constraint fk_appointments_slot foreign key (slot_id) references appointment_slots (id),
    constraint fk_appointments_campus foreign key (campus_id) references campuses (id),
    constraint fk_appointments_room foreign key (room_id) references rooms (id),
    constraint fk_appointments_service_type foreign key (service_type_id) references service_types (id),
    constraint fk_appointments_semester foreign key (semester_id) references school_terms (id),
    constraint fk_appointments_rule_set foreign key (rule_set_id) references appointment_rule_sets (id),
    constraint fk_appointments_consent_record foreign key (consent_record_id) references consent_records (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

alter table appointment_slots
    add constraint fk_slots_appointment foreign key (appointment_id) references appointments (id);

create index idx_appointments_student_status_created on appointments (student_id, status, created_at);
create index idx_appointments_counselor_start on appointments (counselor_id, start_at);
create index idx_appointments_status_created on appointments (status, created_at);
create index idx_appointments_risk_status on appointments (risk_level, status);

create table appointment_forms (
    id bigint primary key auto_increment,
    appointment_id bigint not null,
    first_visit boolean not null,
    issue_types_json json not null,
    description_encrypted blob not null,
    expected_help_encrypted blob null,
    urgency_level varchar(32) not null,
    contact_time varchar(255) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_appointment_forms_appointment unique (appointment_id),
    constraint fk_appointment_forms_appointment foreign key (appointment_id) references appointments (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table risk_assessments (
    id bigint primary key auto_increment,
    appointment_id bigint not null,
    self_harm boolean not null,
    harm_others boolean not null,
    crisis_event boolean not null,
    psychiatric_treatment boolean not null,
    medication boolean not null,
    willing_contact boolean not null,
    risk_level varchar(32) not null,
    review_status varchar(32) not null,
    reviewed_by bigint null,
    reviewed_at datetime(6) null,
    handling_notes_encrypted blob null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_risk_assessments_appointment unique (appointment_id),
    constraint fk_risk_assessments_appointment foreign key (appointment_id) references appointments (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_risk_assessments_level_status on risk_assessments (risk_level, review_status);

create table checkins (
    id bigint primary key auto_increment,
    appointment_id bigint not null,
    student_id bigint not null,
    checkin_at datetime(6) not null,
    method varchar(32) not null,
    late_minutes int not null default 0,
    operator_id bigint null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_checkins_appointment unique (appointment_id),
    constraint fk_checkins_appointment foreign key (appointment_id) references appointments (id),
    constraint fk_checkins_student foreign key (student_id) references students (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table consultation_notes (
    id bigint primary key auto_increment,
    appointment_id bigint not null,
    student_id bigint not null,
    counselor_id bigint not null,
    topic_encrypted blob not null,
    summary_encrypted blob not null,
    risk_change varchar(32) null,
    follow_up_plan_encrypted blob null,
    need_referral boolean not null default false,
    status varchar(32) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint uk_consultation_notes_appointment unique (appointment_id),
    constraint fk_consultation_notes_appointment foreign key (appointment_id) references appointments (id),
    constraint fk_consultation_notes_student foreign key (student_id) references students (id),
    constraint fk_consultation_notes_counselor foreign key (counselor_id) references counselors (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_consultation_notes_student_counselor on consultation_notes (student_id, counselor_id);

create table referrals (
    id bigint primary key auto_increment,
    appointment_id bigint null,
    student_id bigint not null,
    counselor_id bigint null,
    referral_type varchar(32) not null,
    destination varchar(255) not null,
    reason_encrypted blob not null,
    status varchar(32) not null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint fk_referrals_appointment foreign key (appointment_id) references appointments (id),
    constraint fk_referrals_student foreign key (student_id) references students (id),
    constraint fk_referrals_counselor foreign key (counselor_id) references counselors (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table notices (
    id bigint primary key auto_increment,
    title varchar(255) not null,
    content mediumtext not null,
    status varchar(32) not null,
    publish_at datetime(6) null,
    expire_at datetime(6) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table notification_records (
    id bigint primary key auto_increment,
    recipient_account_id bigint not null,
    appointment_id bigint null,
    channel varchar(32) not null,
    template_code varchar(128) not null,
    content_json json not null,
    status varchar(32) not null,
    sent_at datetime(6) null,
    fail_reason varchar(500) null,
    created_at datetime(6) not null default current_timestamp(6),
    updated_at datetime(6) not null default current_timestamp(6) on update current_timestamp(6),
    deleted_at datetime(6) null,
    created_by bigint null,
    updated_by bigint null,
    version int not null default 0,
    constraint fk_notification_records_account foreign key (recipient_account_id) references accounts (id),
    constraint fk_notification_records_appointment foreign key (appointment_id) references appointments (id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create table audit_logs (
    id bigint primary key auto_increment,
    actor_account_id bigint not null,
    action varchar(128) not null,
    target_type varchar(64) not null,
    target_id bigint null,
    sensitive_level varchar(32) not null,
    ip varchar(64) null,
    user_agent varchar(500) null,
    detail_json json not null,
    created_at datetime(6) not null default current_timestamp(6)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_0900_ai_ci;

create index idx_audit_logs_actor_created on audit_logs (actor_account_id, created_at);
create index idx_audit_logs_target on audit_logs (target_type, target_id);
create index idx_audit_logs_sensitive_created on audit_logs (sensitive_level, created_at);

insert into service_types (code, name, duration_minutes, enabled, version)
values
    ('INDIVIDUAL_OFFLINE', '个体线下咨询', 50, true, 0),
    ('INDIVIDUAL_ONLINE', '个体线上咨询', 50, false, 0);

insert into appointment_rule_sets (name, settings_json, is_active, effective_from, published_by, version)
values (
    '默认预约规则',
    json_object(
        'slotDurationMinutes', 50,
        'slotGapMinutes', 10,
        'maxBookingDaysAhead', 14,
        'minBookingHoursAhead', 24,
        'minCancelHoursAhead', 24,
        'maxWeeklyAppointments', 1,
        'maxSemesterCompletedAppointments', 8,
        'maxActiveAppointments', 1,
        'lateMinutes', 15,
        'noShowRestrictThreshold', 2,
        'requireCounselorConfirm', false,
        'enableCheckin', true,
        'highRiskAction', 'RISK_REVIEW'
    ),
    true,
    current_timestamp(6),
    0,
    0
);
