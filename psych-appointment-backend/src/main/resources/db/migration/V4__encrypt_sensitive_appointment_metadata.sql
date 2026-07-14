alter table appointments
    add column cancel_reason_encrypted blob null after cancel_reason;

alter table appointment_forms
    add column metadata_encrypted blob null after issue_types_json,
    modify column issue_types_json json null,
    modify column urgency_level varchar(32) null;

alter table risk_assessments
    add column answers_encrypted blob null after willing_contact,
    add column review_metadata_encrypted blob null after reviewed_at;

alter table referrals
    add column destination_encrypted blob null after destination,
    modify column destination varchar(255) null;
