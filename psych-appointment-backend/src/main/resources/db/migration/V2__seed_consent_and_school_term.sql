insert into consent_versions (version_no, title, content, status, published_at, version)
select 'v1', '心理咨询知情同意书', '学生已阅读并同意心理咨询服务须知、保密原则及保密例外。', 'PUBLISHED', current_timestamp(6), 0
where not exists (select 1 from consent_versions where version_no = 'v1');

insert into school_terms (name, start_date, end_date, is_current, status, version)
select '2026 学年联调学期', '2026-01-01', '2026-12-31', true, 'ACTIVE', 0
where not exists (select 1 from school_terms where is_current = true and status = 'ACTIVE');
