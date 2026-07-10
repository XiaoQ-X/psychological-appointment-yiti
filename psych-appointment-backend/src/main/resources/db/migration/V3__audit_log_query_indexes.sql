create index idx_audit_logs_created on audit_logs (created_at);
create index idx_audit_logs_action_created on audit_logs (action, created_at);
