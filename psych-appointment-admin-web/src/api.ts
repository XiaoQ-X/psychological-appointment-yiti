export type ApiResponse<T> = {
  success: boolean
  code: string
  message: string
  data: T
}

export type AdminSession = {
  accessToken: string
  tokenType: string
  expiresAtEpochSeconds: number
  accountId: number
  username: string
  role: string
  forcePasswordChange: boolean
}

export type Campus = {
  id: number
  name: string
  address?: string
  status: string
}

export type Room = {
  id: number
  campusId: number
  campusName?: string
  name: string
  locationDesc: string
  status: string
}

export type Counselor = {
  id: number
  accountId: number
  username: string
  name: string
  avatarUrl?: string
  title?: string
  gender?: string
  campusId?: number
  campusName?: string
  expertise: string[]
  intro?: string
  trainingBackground?: string
  serviceModes: string[]
  maxDailyCount: number
  visible: boolean
  status: string
}

export type ServiceType = {
  id: number
  code: string
  name: string
  durationMinutes: number
  enabled: boolean
}

export type ScheduleTemplate = {
  id: number
  counselorId: number
  counselorName?: string
  campusId: number
  campusName?: string
  roomId?: number
  roomName?: string
  serviceTypeId: number
  serviceTypeName?: string
  dayOfWeek: number
  startTime: string
  endTime: string
  effectiveFrom: string
  effectiveTo?: string
  status: string
}

export type GenerateSlotsResponse = {
  startDate: string
  endDate: string
  counselorId?: number
  generatedCount: number
  existingCount: number
  skippedPastCount: number
  skippedDisabledServiceCount: number
  skippedLimitCount: number
}

export type AppointmentRecord = {
  appointmentId: number
  appointmentNo: string
  status: string
  riskLevel: string
  riskReviewStatus?: string
  studentId: number
  studentNo?: string
  studentName?: string
  college?: string
  grade?: string
  counselorId: number
  counselorName?: string
  campusId: number
  campusName?: string
  roomId?: number
  roomName?: string
  serviceTypeId: number
  serviceTypeName?: string
  startAt: string
  endAt: string
  canceledAt?: string
  completedAt?: string
}

export type AppointmentDetail = AppointmentRecord & {
  gender?: string
  major?: string
  className?: string
  counselorTitle?: string
  firstVisit: boolean
  issueTypes: string[]
  urgencyLevel?: string
  contactTime?: string
  selfHarm: boolean
  harmOthers: boolean
  crisisEvent: boolean
  psychiatricTreatment: boolean
  medication: boolean
  willingContact: boolean
  riskReviewedBy?: number
  riskReviewedAt?: string
  referralId?: number
  referralType?: string
  referralDestination?: string
  referralStatus?: string
  cancelReason?: string
}

export type StudentImportRow = {
  rowNo: number
  studentNo?: string
  name?: string
  status: string
  message: string
}

export type StudentImportResponse = {
  batchId: number
  totalRows: number
  successCount: number
  skippedCount: number
  failedCount: number
  rows: StudentImportRow[]
}

export type CreateCounselorPayload = {
  username: string
  initialPassword: string
  name: string
  title: string
  gender: string
  campusId?: number
  expertise: string[]
  intro: string
  trainingBackground: string
  serviceModes: string[]
  maxDailyCount: number
  visible: boolean
  status: string
}

export type CreateSchedulePayload = {
  counselorId: number
  campusId: number
  roomId?: number
  serviceTypeId: number
  dayOfWeek: number
  startTime: string
  endTime: string
  effectiveFrom: string
  effectiveTo?: string
  status: string
}

export type AppointmentRuleSettings = {
  slotGapMinutes: number
  slotLockMinutes: number
  maxBookingDaysAhead: number
  minBookingHoursAhead: number
  minCancelHoursAhead: number
  maxWeeklyAppointments: number
  maxSemesterCompletedAppointments: number
  maxActiveAppointments: number
  noShowRestrictThreshold: number
}

export type AppointmentRule = {
  id: number
  name: string
  settings: AppointmentRuleSettings
  active: boolean
  effectiveFrom: string
  effectiveTo?: string
  publishedBy: number
  publishedByUsername?: string
  createdAt: string
  updatedAt: string
  version: number
}

export type AuditLog = {
  id: number
  actorAccountId: number
  actorUsername?: string
  action: string
  targetType: string
  targetId?: number
  sensitiveLevel: 'NORMAL' | 'SENSITIVE' | 'HIGH'
  ip?: string
  userAgent?: string
  detail: Record<string, unknown>
  createdAt: string
}

export type AuditLogPage = {
  items: AuditLog[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || ''

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

export async function apiRequest<T>(
  path: string,
  options: RequestInit = {},
  token?: string,
): Promise<T> {
  const headers = new Headers(options.headers)
  const isFormData = options.body instanceof FormData
  if (!isFormData && options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
  })
  const body = (await response.json().catch(() => null)) as ApiResponse<T> | null
  if (!response.ok || body?.success === false) {
    throw new ApiError(body?.message || `请求失败 ${response.status}`, response.status)
  }
  return body?.data as T
}

export function loginAdmin(username: string, password: string) {
  return apiRequest<AdminSession>('/api/admin/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  })
}

export function importStudents(file: File, token: string) {
  const formData = new FormData()
  formData.append('file', file)
  return apiRequest<StudentImportResponse>(
    '/api/admin/students/import',
    { method: 'POST', body: formData },
    token,
  )
}
