# School Psychological Appointment Backend

Spring Boot backend skeleton for the school psychological counseling appointment mini program.

## Stack

- Java 17
- Spring Boot 3.5.x
- MySQL 8
- Redis
- Flyway
- Spring Security

## Local Services

Configure local environment variables from `.env.example`, then start MySQL and Redis:

```powershell
$env:MYSQL_HOST_PORT = "13306"  # use this when local 3306 is already occupied
$env:REDIS_HOST_PORT = "16380"  # use this when local 6379 is already occupied
docker compose up -d
```

## Run

Spring Boot reads environment variables from the shell or IDE run configuration. For local PowerShell runs, set at least these values:

```powershell
$bytes = New-Object byte[] 32
[Security.Cryptography.RandomNumberGenerator]::Fill($bytes)
$env:SENSITIVE_DATA_KEY_BASE64 = [Convert]::ToBase64String($bytes)

$env:TOKEN_SECRET = "replace-with-at-least-32-random-characters"
$env:DB_PASSWORD = "change-me-db"
$env:REDIS_PASSWORD = "change-me-redis"
$env:BOOTSTRAP_ADMIN_USERNAME = "admin"
$env:BOOTSTRAP_ADMIN_PASSWORD = "change-me-admin"
$env:SERVER_PORT = "18080"
```

```powershell
mvn spring-boot:run
```

Default local endpoints:

- `GET /api/health`
- `GET /actuator/health`

## Admin Login

```powershell
$body = @{ username = "admin"; password = "change-me-admin" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/admin/auth/login" -ContentType "application/json" -Body $body
```

## Student Excel Import

Endpoint:

```text
POST /api/admin/students/import
Authorization: Bearer <accessToken>
Content-Type: multipart/form-data
```

Required Excel headers:

| Header | Required | Notes |
| --- | --- | --- |
| 学号 | Yes | Used as login username |
| 姓名 | Yes | Student real name |
| 初始密码 | Yes | At least 8 characters; stored as BCrypt hash |
| 学院 | Yes | Used for management and reporting |
| 专业 | No | Optional |
| 班级 | No | Optional |
| 年级 | No | Optional |
| 性别 | No | Optional |
| 手机号 | No | Encrypted with AES-GCM when present |
| 状态 | No | Defaults to ACTIVE; use 禁用/DISABLED to disable |

English aliases are also accepted: `studentNo`, `name`, `password`, `college`, `major`, `class`, `grade`, `gender`, `phone`, `status`.

PowerShell upload example:

```powershell
$token = "<accessToken>"
curl.exe -X POST "http://localhost:8080/api/admin/students/import" `
  -H "Authorization: Bearer $token" `
  -F "file=@D:\path\students.xlsx"
```

## Student Login

```powershell
$body = @{ username = "202607040001"; password = "Student123!" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:8080/api/student/auth/login" -ContentType "application/json" -Body $body
```

## Submit Appointment

Endpoint:

```text
POST /api/student/appointments
Authorization: Bearer <studentAccessToken>
Content-Type: application/json
```

Recommended student booking flow:

1. Query slots: `GET /api/student/counselors/{id}/slots`
2. Lock the selected slot: `POST /api/student/appointments/slots/{slotId}/lock`
3. Submit the appointment form: `POST /api/student/appointments`

Lock slot endpoint:

```text
POST /api/student/appointments/slots/{slotId}/lock
Authorization: Bearer <studentAccessToken>
```

The default lock duration is 10 minutes. If the active rule set contains `slotLockMinutes`, that value is used instead. Locking a new slot releases the same student's other pending slot locks.

Example:

```json
{
  "slotId": 1,
  "firstVisit": true,
  "issueTypes": ["study-pressure"],
  "description": "Need support for academic stress.",
  "expectedHelp": "Understand pressure and plan next steps.",
  "urgencyLevel": "LOW",
  "contactTime": "weekday afternoon",
  "consentVersionId": 1,
  "consentAgreed": true,
  "risk": {
    "selfHarm": false,
    "harmOthers": false,
    "crisisEvent": false,
    "psychiatricTreatment": false,
    "medication": false,
    "willingContact": true
  }
}
```

Low/medium-risk appointments are confirmed immediately. High-risk submissions are stored with `RISK_REVIEW` status and the slot is still reserved for follow-up handling.

The student appointment detail response includes `canCancel`, `minCancelHoursAhead`, and `cancelDeadline`. Clients should use these server-calculated fields to control cancellation availability instead of calculating the active rule locally.

The latest published consent version is available without authentication:

```text
GET /api/public/consent/current
```

Cancel appointment endpoint:

```text
POST /api/student/appointments/{appointmentId}/cancel
Authorization: Bearer <studentAccessToken>
Content-Type: application/json
```

Example:

```json
{
  "reason": "Schedule conflict"
}
```

Student cancellation releases the booked slot back to `AVAILABLE`. The default minimum cancellation window is 24 hours before the appointment start. If the active rule set contains `minCancelHoursAhead`, that value is used instead.

## Admin Counselor, Schedule, and Slot Management

All endpoints require:

```text
Authorization: Bearer <adminAccessToken>
```

Core setup flow:

1. Create or list campuses: `POST /api/admin/campuses`, `GET /api/admin/campuses`
2. Create or list rooms: `POST /api/admin/rooms`, `GET /api/admin/rooms?campusId=1`
3. Create or list counselors: `POST /api/admin/counselors`, `GET /api/admin/counselors`
4. List service types: `GET /api/admin/service-types`
5. Create schedule templates: `POST /api/admin/schedules`
6. Generate concrete available slots: `POST /api/admin/slots/generate`

Create counselor example:

```json
{
  "username": "counselor_001",
  "initialPassword": "Counselor123!",
  "name": "Counselor One",
  "title": "Psychological Counselor",
  "gender": "FEMALE",
  "campusId": 1,
  "expertise": ["academic stress", "emotion"],
  "intro": "Focuses on student adaptation and stress support.",
  "trainingBackground": "School counseling training.",
  "serviceModes": ["OFFLINE"],
  "maxDailyCount": 4,
  "visible": true,
  "status": "ACTIVE"
}
```

Create weekly schedule template example. `dayOfWeek` uses ISO values: `1` Monday through `7` Sunday.

```json
{
  "counselorId": 1,
  "campusId": 1,
  "roomId": 1,
  "serviceTypeId": 1,
  "dayOfWeek": 1,
  "startTime": "09:00:00",
  "endTime": "12:00:00",
  "effectiveFrom": "2026-07-01",
  "effectiveTo": "2026-12-31",
  "status": "ACTIVE"
}
```

Generate slots example:

```json
{
  "startDate": "2026-07-06",
  "endDate": "2026-07-12",
  "counselorId": 1
}
```

Slot generation is idempotent for the same counselor and time range. Existing slots are counted in `existingCount` instead of being inserted again.

## Appointment Rule Management

All endpoints require an administrator access token:

- `GET /api/admin/appointment-rules`
- `POST /api/admin/appointment-rules`
- `PUT /api/admin/appointment-rules/{ruleId}`
- `POST /api/admin/appointment-rules/{ruleId}/activate`

Rules are versioned. Drafts can be edited; active and historical versions are immutable. Activating a version closes the previous active version in the same transaction. Managed settings cover slot gap and lock time, booking lead/window limits, cancellation lead time, weekly and semester limits, and the maximum number of active appointments.

## Audit Logs

Critical administrator operations are written to `audit_logs` in the same transaction as the business change. Passwords, consultation descriptions, handling notes, and referral reasons are not copied into audit details.

Query endpoint:

```text
GET /api/admin/audit-logs?action=RISK_REVIEWED&sensitiveLevel=HIGH&page=0&size=20
Authorization: Bearer <adminAccessToken>
```

Supported filters are `actorAccountId`, `action`, `targetType`, `sensitiveLevel`, `from`, and `to`.

## Student Availability Query

All endpoints require:

```text
Authorization: Bearer <studentAccessToken>
```

Endpoints:

- `GET /api/student/counselors?from=2026-07-06&to=2026-07-12`
- `GET /api/student/counselors/{id}`
- `GET /api/student/counselors/{id}/slots?from=2026-07-06&to=2026-07-12`

Only active and visible counselors are returned. Slot queries only return future `AVAILABLE` slots that were generated by the admin workflow.

## Student Appointment Records

All endpoints require:

```text
Authorization: Bearer <studentAccessToken>
```

Endpoints:

- `GET /api/student/appointments`
- `GET /api/student/appointments?status=CONFIRMED&from=2026-07-01&to=2026-07-31`
- `GET /api/student/appointments/{appointmentId}`

The detail endpoint returns appointment status, time, counselor, campus, room, service type, risk level, and non-sensitive form fields such as first visit, issue types, urgency level, and contact time.

## Counselor Login

```powershell
$body = @{ username = "counselor_001"; password = "Counselor123!" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "http://localhost:18080/api/counselor/auth/login" -ContentType "application/json" -Body $body
```

The response includes `accessToken`, `accountId`, `counselorId`, `counselorName`, and `forcePasswordChange`.

## Counselor Appointment Query

All endpoints require:

```text
Authorization: Bearer <counselorAccessToken>
```

Endpoints:

- `GET /api/counselor/appointments`
- `GET /api/counselor/appointments?status=CONFIRMED&from=2026-07-01&to=2026-07-31`
- `GET /api/counselor/appointments/{appointmentId}`

Counselors can only query appointments assigned to their own `counselorId`. The detail endpoint includes student basic information and non-sensitive appointment form fields. Encrypted description fields are not returned by this API.

## Complete Consultation

Endpoint:

```text
POST /api/counselor/appointments/{appointmentId}/complete
Authorization: Bearer <counselorAccessToken>
Content-Type: application/json
```

Example:

```json
{
  "topic": "Academic stress adjustment",
  "summary": "Discussed current academic stressors and coping plan.",
  "riskChange": "STABLE",
  "followUpPlan": "Recommend one follow-up session next week.",
  "needReferral": false
}
```

Only the assigned counselor can complete the appointment. The appointment must have started and be in `CONFIRMED` or `CHECKED_IN` status. Completion writes a submitted consultation note to `consultation_notes`, encrypting `topic`, `summary`, and `followUpPlan`, then marks the appointment as `COMPLETED`.

## Mark Student No-show

Endpoint:

```text
POST /api/counselor/appointments/{appointmentId}/no-show
Authorization: Bearer <counselorAccessToken>
```

Only the assigned counselor can mark an appointment as `NO_SHOW`. The appointment must be `CONFIRMED` and its end time must have passed. The operation increments the student's no-show count and writes an `APPOINTMENT_MARKED_NO_SHOW` sensitive audit log in the same transaction.

Expired appointments are excluded from the student's active-appointment limit even before the counselor records the final `COMPLETED` or `NO_SHOW` outcome. The system does not automatically classify expired appointments as no-shows.

## Admin Appointment Management

All endpoints require:

```text
Authorization: Bearer <adminAccessToken>
```

Appointment query endpoints:

- `GET /api/admin/appointments`
- `GET /api/admin/appointments?status=RISK_REVIEW&riskLevel=HIGH`
- `GET /api/admin/appointments?studentNo=202607040002&from=2026-07-01&to=2026-07-31`
- `GET /api/admin/appointments/{appointmentId}`

The detail endpoint returns appointment status, student/counselor/campus/room/service information, risk flags, risk review status, referral summary, and non-sensitive form fields. Encrypted description and handling-note fields are not returned by this API.

Admin cancel endpoint:

```text
POST /api/admin/appointments/{appointmentId}/cancel
Authorization: Bearer <adminAccessToken>
Content-Type: application/json
```

```json
{
  "reason": "Administrative cancellation"
}
```

Admin cancellation marks the appointment as `CANCELED_BY_ADMIN` and releases the booked slot when applicable.

High-risk review endpoint:

```text
POST /api/admin/appointments/{appointmentId}/risk-review
Authorization: Bearer <adminAccessToken>
Content-Type: application/json
```

Approve and continue as a normal appointment:

```json
{
  "decision": "APPROVE",
  "handlingNotes": "Reviewed by duty administrator; continue with scheduled counseling."
}
```

Refer outside normal counseling:

```json
{
  "decision": "REFER",
  "handlingNotes": "High-risk referral required.",
  "referralType": "HOSPITAL",
  "referralDestination": "City Mental Health Center",
  "referralReason": "Needs specialist risk evaluation."
}
```

Close the high-risk appointment:

```json
{
  "decision": "CLOSE",
  "handlingNotes": "Closed after emergency handling."
}
```

Review rules:

- `APPROVE` moves the appointment from `RISK_REVIEW` to `CONFIRMED` and keeps the slot booked.
- `REFER` creates an `OPEN` referral, moves the appointment to `REFERRED`, and releases the slot.
- `CLOSE` moves the appointment to `CLOSED` and releases the slot.
- `handlingNotes` and `referralReason` are encrypted before storage.

## Security Notes

- Initial student passwords must be hashed before storage.
- Sensitive text fields are modeled as encrypted binary columns.
- Sensitive reads and exports should be recorded in `audit_logs`.
- Production authentication should use short-lived access tokens plus refresh-token rotation.
