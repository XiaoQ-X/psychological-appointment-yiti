import { useCallback, useEffect, useMemo, useState } from 'react'
import './App.css'
import {
  apiRequest,
  importStudents,
  loginAdmin,
  type AdminSession,
  type AppointmentRule,
  type AppointmentRuleSettings,
  type AppointmentDetail,
  type AppointmentRecord,
  type AuditLogPage,
  type Campus,
  type Counselor,
  type CreateCounselorPayload,
  type CreateSchedulePayload,
  type GenerateSlotsResponse,
  type Room,
  type ScheduleTemplate,
  type ServiceType,
  type StudentImportResponse,
} from './api'

type ViewKey = 'students' | 'counselors' | 'schedules' | 'appointments' | 'risk' | 'rules' | 'audit'

const SESSION_KEY = 'psych_admin_session'
const navItems: Array<{ key: ViewKey; label: string; hint: string }> = [
  { key: 'students', label: '学生导入', hint: 'Excel 批量初始化账号' },
  { key: 'counselors', label: '咨询师管理', hint: '账号、校区、可见状态' },
  { key: 'schedules', label: '排班时段', hint: '模板与时段生成' },
  { key: 'appointments', label: '预约管理', hint: '查询、详情、状态跟进' },
  { key: 'risk', label: '高风险审核', hint: '审核、转介、关闭' },
  { key: 'rules', label: '预约规则', hint: '版本维护与动态启用' },
  { key: 'audit', label: '审计日志', hint: '关键操作留痕查询' },
]

const defaultRuleSettings: AppointmentRuleSettings = {
  slotGapMinutes: 10,
  slotLockMinutes: 10,
  maxBookingDaysAhead: 14,
  minBookingHoursAhead: 24,
  minCancelHoursAhead: 24,
  maxWeeklyAppointments: 1,
  maxSemesterCompletedAppointments: 8,
  maxActiveAppointments: 1,
}

const emptyRuleForm = {
  name: '学校默认预约规则',
  settings: defaultRuleSettings,
}

const emptyCounselorForm = {
  username: '',
  initialPassword: '',
  name: '',
  title: 'Psychological Counselor',
  gender: 'FEMALE',
  campusId: '',
  expertise: 'academic stress, emotion',
  intro: 'Focuses on student adaptation and stress support.',
  trainingBackground: 'School counseling training.',
  maxDailyCount: 4,
  visible: true,
  status: 'ACTIVE',
}

const emptyScheduleForm = {
  counselorId: '',
  campusId: '',
  roomId: '',
  serviceTypeId: '',
  dayOfWeek: '1',
  startTime: '09:00:00',
  endTime: '12:00:00',
  effectiveFrom: toDateInput(new Date()),
  effectiveTo: '',
  status: 'ACTIVE',
}

function App() {
  const [session, setSession] = useState<AdminSession | null>(() => readSession())
  const [activeView, setActiveView] = useState<ViewKey>('students')
  const [loginForm, setLoginForm] = useState({ username: 'admin', password: '' })
  const [loginLoading, setLoginLoading] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const [campuses, setCampuses] = useState<Campus[]>([])
  const [rooms, setRooms] = useState<Room[]>([])
  const [counselors, setCounselors] = useState<Counselor[]>([])
  const [serviceTypes, setServiceTypes] = useState<ServiceType[]>([])
  const [schedules, setSchedules] = useState<ScheduleTemplate[]>([])
  const [appointments, setAppointments] = useState<AppointmentRecord[]>([])
  const [appointmentResults, setAppointmentResults] = useState<AppointmentRecord[]>([])
  const [selectedAppointment, setSelectedAppointment] = useState<AppointmentDetail | null>(null)
  const [rules, setRules] = useState<AppointmentRule[]>([])
  const [editingRuleId, setEditingRuleId] = useState<number | null>(null)
  const [ruleForm, setRuleForm] = useState(emptyRuleForm)
  const [auditPage, setAuditPage] = useState<AuditLogPage>({
    items: [], page: 0, size: 20, totalElements: 0, totalPages: 0,
  })
  const [auditFilters, setAuditFilters] = useState({
    action: '',
    targetType: '',
    sensitiveLevel: '',
    actorAccountId: '',
    from: '',
    to: '',
  })

  const [studentFile, setStudentFile] = useState<File | null>(null)
  const [importResult, setImportResult] = useState<StudentImportResponse | null>(null)
  const [counselorForm, setCounselorForm] = useState(emptyCounselorForm)
  const [scheduleForm, setScheduleForm] = useState(emptyScheduleForm)
  const [slotForm, setSlotForm] = useState({
    startDate: toDateInput(new Date()),
    endDate: toDateInput(addDays(new Date(), 7)),
    counselorId: '',
  })
  const [slotResult, setSlotResult] = useState<GenerateSlotsResponse | null>(null)
  const [appointmentFilters, setAppointmentFilters] = useState({
    status: '',
    riskLevel: '',
    studentNo: '',
    counselorId: '',
    from: '',
    to: '',
  })
  const [riskForm, setRiskForm] = useState({
    decision: 'APPROVE',
    handlingNotes: 'Reviewed by duty administrator; continue with scheduled counseling.',
    referralType: 'HOSPITAL',
    referralDestination: 'City Mental Health Center',
    referralReason: 'Needs specialist risk evaluation.',
  })

  const token = session?.accessToken
  const riskAppointments = useMemo(
    () =>
      appointments.filter(
        (item) => item.status === 'RISK_REVIEW' || item.riskLevel === 'HIGH',
      ),
    [appointments],
  )
  const pendingRiskCount = appointments.filter((item) => item.status === 'RISK_REVIEW').length
  const activeAppointmentCount = appointments.filter((item) =>
    ['SUBMITTED', 'RISK_REVIEW', 'COUNSELOR_REVIEW', 'ADMIN_REVIEW', 'CONFIRMED', 'CHECKED_IN'].includes(
      item.status,
    ) && Boolean(item.endAt && new Date(item.endAt).getTime() >= Date.now()),
  ).length

  async function handleLogin(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setLoginLoading(true)
    clearNotice()
    try {
      const loginData = await loginAdmin(loginForm.username.trim(), loginForm.password)
      localStorage.setItem(SESSION_KEY, JSON.stringify(loginData))
      setSession(loginData)
      setMessage('管理员登录成功')
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setLoginLoading(false)
    }
  }

  const loadAll = useCallback(async (accessToken = token) => {
    if (!accessToken) {
      return
    }
    setBusy(true)
    clearNotice()
    try {
      const [
        nextCampuses,
        nextRooms,
        nextCounselors,
        nextServiceTypes,
        nextSchedules,
        nextAppointments,
        nextRules,
        nextAuditPage,
      ] =
        await Promise.all([
          apiRequest<Campus[]>('/api/admin/campuses', {}, accessToken),
          apiRequest<Room[]>('/api/admin/rooms', {}, accessToken),
          apiRequest<Counselor[]>('/api/admin/counselors', {}, accessToken),
          apiRequest<ServiceType[]>('/api/admin/service-types', {}, accessToken),
          apiRequest<ScheduleTemplate[]>('/api/admin/schedules', {}, accessToken),
          apiRequest<AppointmentRecord[]>('/api/admin/appointments', {}, accessToken),
          apiRequest<AppointmentRule[]>('/api/admin/appointment-rules', {}, accessToken),
          apiRequest<AuditLogPage>('/api/admin/audit-logs?page=0&size=20', {}, accessToken),
        ])
      setCampuses(nextCampuses)
      setRooms(nextRooms)
      setCounselors(nextCounselors)
      setServiceTypes(nextServiceTypes)
      setSchedules(nextSchedules)
      setAppointments(nextAppointments)
      setAppointmentResults(nextAppointments)
      setRules(nextRules)
      setAuditPage(nextAuditPage)
      const activeRule = nextRules.find((rule) => rule.active)
      if (activeRule) {
        setRuleForm({ name: activeRule.name, settings: activeRule.settings })
        setEditingRuleId(null)
      }
      if (!scheduleForm.campusId && nextCampuses[0]) {
        setScheduleForm((current) => ({
          ...current,
          campusId: String(nextCampuses[0].id),
          roomId: nextRooms.find((room) => room.campusId === nextCampuses[0].id)?.id.toString() || '',
          serviceTypeId: nextServiceTypes[0]?.id.toString() || '',
          counselorId: nextCounselors[0]?.id.toString() || '',
        }))
      }
      if (!counselorForm.campusId && nextCampuses[0]) {
        setCounselorForm((current) => ({ ...current, campusId: String(nextCampuses[0].id) }))
      }
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }, [counselorForm.campusId, scheduleForm.campusId, token])

  useEffect(() => {
    if (session?.accessToken) {
      void loadAll(session.accessToken)
    }
  }, [loadAll, session?.accessToken])

  async function handleStudentImport(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!studentFile || !token) {
      setError('请选择 Excel 文件')
      return
    }
    setBusy(true)
    clearNotice()
    try {
      const result = await importStudents(studentFile, token)
      setImportResult(result)
      setMessage(`导入完成：成功 ${result.successCount} 条，失败 ${result.failedCount} 条`)
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function handleCreateCounselor(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) {
      return
    }
    const payload: CreateCounselorPayload = {
      username: counselorForm.username.trim(),
      initialPassword: counselorForm.initialPassword,
      name: counselorForm.name.trim(),
      title: counselorForm.title,
      gender: counselorForm.gender,
      campusId: numberOrUndefined(counselorForm.campusId),
      expertise: splitList(counselorForm.expertise),
      intro: counselorForm.intro,
      trainingBackground: counselorForm.trainingBackground,
      serviceModes: ['OFFLINE'],
      maxDailyCount: Number(counselorForm.maxDailyCount) || 0,
      visible: counselorForm.visible,
      status: counselorForm.status,
    }
    setBusy(true)
    clearNotice()
    try {
      await apiRequest<Counselor>(
        '/api/admin/counselors',
        { method: 'POST', body: JSON.stringify(payload) },
        token,
      )
      setCounselorForm(emptyCounselorForm)
      await loadAll(token)
      setMessage('咨询师创建成功')
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function handleCreateSchedule(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) {
      return
    }
    const payload: CreateSchedulePayload = {
      counselorId: Number(scheduleForm.counselorId),
      campusId: Number(scheduleForm.campusId),
      roomId: numberOrUndefined(scheduleForm.roomId),
      serviceTypeId: Number(scheduleForm.serviceTypeId),
      dayOfWeek: Number(scheduleForm.dayOfWeek),
      startTime: scheduleForm.startTime,
      endTime: scheduleForm.endTime,
      effectiveFrom: scheduleForm.effectiveFrom,
      effectiveTo: scheduleForm.effectiveTo || undefined,
      status: scheduleForm.status,
    }
    setBusy(true)
    clearNotice()
    try {
      await apiRequest<ScheduleTemplate>(
        '/api/admin/schedules',
        { method: 'POST', body: JSON.stringify(payload) },
        token,
      )
      await loadAll(token)
      setMessage('排班模板创建成功')
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function handleGenerateSlots(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token) {
      return
    }
    setBusy(true)
    clearNotice()
    try {
      const result = await apiRequest<GenerateSlotsResponse>(
        '/api/admin/slots/generate',
        {
          method: 'POST',
          body: JSON.stringify({
            startDate: slotForm.startDate,
            endDate: slotForm.endDate,
            counselorId: numberOrUndefined(slotForm.counselorId),
          }),
        },
        token,
      )
      setSlotResult(result)
      setMessage(`时段生成完成：新增 ${result.generatedCount} 个，已有 ${result.existingCount} 个`)
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function loadAppointments(event?: React.FormEvent<HTMLFormElement>) {
    event?.preventDefault()
    if (!token) {
      return
    }
    setBusy(true)
    clearNotice()
    try {
      const query = new URLSearchParams()
      Object.entries(appointmentFilters).forEach(([key, value]) => {
        if (value) {
          query.set(key, value)
        }
      })
      const suffix = query.toString() ? `?${query.toString()}` : ''
      const data = await apiRequest<AppointmentRecord[]>(`/api/admin/appointments${suffix}`, {}, token)
      setAppointmentResults(data)
      setMessage(`已加载 ${data.length} 条预约`)
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function openAppointment(id: number) {
    if (!token) {
      return
    }
    setBusy(true)
    clearNotice()
    try {
      const detail = await apiRequest<AppointmentDetail>(`/api/admin/appointments/${id}`, {}, token)
      setSelectedAppointment(detail)
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function handleRiskReview(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!token || !selectedAppointment) {
      return
    }
    const body =
      riskForm.decision === 'REFER'
        ? riskForm
        : {
            decision: riskForm.decision,
            handlingNotes: riskForm.handlingNotes,
          }
    setBusy(true)
    clearNotice()
    try {
      await apiRequest(
        `/api/admin/appointments/${selectedAppointment.appointmentId}/risk-review`,
        { method: 'POST', body: JSON.stringify(body) },
        token,
      )
      await loadAll(token)
      await openAppointment(selectedAppointment.appointmentId)
      setMessage('高风险审核已提交')
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function saveRule(activate: boolean) {
    if (!token) {
      return
    }
    setBusy(true)
    clearNotice()
    try {
      const path = editingRuleId
        ? `/api/admin/appointment-rules/${editingRuleId}`
        : '/api/admin/appointment-rules'
      const savedRule = await apiRequest<AppointmentRule>(
        path,
        {
          method: editingRuleId ? 'PUT' : 'POST',
          body: JSON.stringify(ruleForm),
        },
        token,
      )
      if (activate) {
        await apiRequest<AppointmentRule>(
          `/api/admin/appointment-rules/${savedRule.id}/activate`,
          { method: 'POST' },
          token,
        )
      }
      setEditingRuleId(null)
      await loadAll(token)
      setMessage(activate ? '预约规则已保存并启用' : '预约规则草稿已保存')
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  async function activateRule(id: number) {
    if (!token) {
      return
    }
    setBusy(true)
    clearNotice()
    try {
      await apiRequest<AppointmentRule>(
        `/api/admin/appointment-rules/${id}/activate`,
        { method: 'POST' },
        token,
      )
      await loadAll(token)
      setMessage('预约规则已启用，后续查询和预约将按新规则执行')
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  function editRule(rule: AppointmentRule) {
    setEditingRuleId(!rule.active && !rule.effectiveTo ? rule.id : null)
    setRuleForm({ name: rule.name, settings: rule.settings })
    setActiveView('rules')
    clearNotice()
  }

  async function loadAuditLogs(page = 0, event?: React.FormEvent<HTMLFormElement>) {
    event?.preventDefault()
    if (!token) {
      return
    }
    setBusy(true)
    clearNotice()
    try {
      const query = new URLSearchParams({ page: String(page), size: '20' })
      Object.entries(auditFilters).forEach(([key, value]) => {
        if (value) {
          query.set(key, value)
        }
      })
      const data = await apiRequest<AuditLogPage>(`/api/admin/audit-logs?${query.toString()}`, {}, token)
      setAuditPage(data)
    } catch (caught) {
      setError(errorMessage(caught))
    } finally {
      setBusy(false)
    }
  }

  function logout() {
    localStorage.removeItem(SESSION_KEY)
    setSession(null)
    setSelectedAppointment(null)
    setMessage('')
    setError('')
  }

  function clearNotice() {
    setMessage('')
    setError('')
  }

  if (!session) {
    return (
      <main className="login-shell">
        <section className="login-visual" aria-label="心理中心后台介绍">
          <span className="visual-kicker">校园心理支持 · 管理工作台</span>
          <h2>让每一次求助，<br />都有人回应。</h2>
          <p>在清晰的流程里守护学生，在可靠的记录中支持咨询师。</p>
          <div className="visual-lines" aria-hidden="true">
            <span className="line line-one" />
            <span className="line line-two" />
            <span className="line line-three" />
            <span className="line-dot" />
          </div>
          <div className="visual-footer">
            <span>本校专用</span>
            <span>隐私优先</span>
            <span>及时响应</span>
          </div>
        </section>
        <form className="login-panel" onSubmit={handleLogin}>
          <div className="login-form-head">
            <div className="brand-mark">管</div>
            <span className="login-eyebrow">COUNSELING CENTER</span>
          </div>
          <h1>登录心理预约后台</h1>
          <p>管理员登录后可导入学生、维护咨询师、生成排班时段并处理高风险预约。</p>
          <label>
            账号
            <input
              value={loginForm.username}
              onChange={(event) => setLoginForm({ ...loginForm, username: event.target.value })}
              autoComplete="username"
            />
          </label>
          <label>
            密码
            <input
              value={loginForm.password}
              onChange={(event) => setLoginForm({ ...loginForm, password: event.target.value })}
              type="password"
              autoComplete="current-password"
            />
          </label>
          {error ? <div className="notice error">{error}</div> : null}
          <button className="primary-button" type="submit" disabled={loginLoading}>
            {loginLoading ? '登录中' : '登录后台'}
          </button>
        </form>
      </main>
    )
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <div className="brand-mark small">心</div>
          <div>
            <strong>心理预约后台</strong>
            <span>{session.username}</span>
          </div>
        </div>
        <nav className="nav-list">
          {navItems.map((item) => (
            <button
              className={activeView === item.key ? 'nav-item active' : 'nav-item'}
              key={item.key}
              type="button"
              onClick={() => setActiveView(item.key)}
            >
              <span>{item.label}</span>
              <small>{item.hint}</small>
            </button>
          ))}
        </nav>
        <button className="ghost-button" type="button" onClick={logout}>
          退出登录
        </button>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <h1>{navItems.find((item) => item.key === activeView)?.label}</h1>
            <p>后端：Spring Boot + MySQL + Redis，本页通过真实接口联调。</p>
          </div>
          <button className="secondary-button" type="button" onClick={() => void loadAll()}>
            {busy ? '同步中' : '刷新数据'}
          </button>
        </header>

        <section className="metric-grid">
          <Metric label="咨询师" value={counselors.length} />
          <Metric label="排班模板" value={schedules.length} />
          <Metric label="预约记录" value={appointments.length} />
          <Metric label="待风险审核" value={pendingRiskCount} tone={pendingRiskCount ? 'warning' : 'normal'} />
        </section>

        {message ? <div className="notice success">{message}</div> : null}
        {error ? <div className="notice error">{error}</div> : null}

        {activeView === 'students' ? (
          <StudentsSection
            busy={busy}
            importResult={importResult}
            onFileChange={setStudentFile}
            onSubmit={handleStudentImport}
          />
        ) : null}

        {activeView === 'counselors' ? (
          <CounselorsSection
            campuses={campuses}
            counselors={counselors}
            form={counselorForm}
            onChange={setCounselorForm}
            onSubmit={handleCreateCounselor}
          />
        ) : null}

        {activeView === 'schedules' ? (
          <SchedulesSection
            campuses={campuses}
            counselors={counselors}
            rooms={rooms}
            serviceTypes={serviceTypes}
            schedules={schedules}
            scheduleForm={scheduleForm}
            slotForm={slotForm}
            slotResult={slotResult}
            onScheduleChange={setScheduleForm}
            onSlotChange={setSlotForm}
            onCreateSchedule={handleCreateSchedule}
            onGenerateSlots={handleGenerateSlots}
          />
        ) : null}

        {activeView === 'appointments' ? (
          <AppointmentsSection
            appointments={appointmentResults}
            filters={appointmentFilters}
            activeCount={activeAppointmentCount}
            onFiltersChange={setAppointmentFilters}
            onLoad={loadAppointments}
            onOpen={openAppointment}
          />
        ) : null}

        {activeView === 'risk' ? (
          <RiskSection
            appointments={riskAppointments}
            selected={selectedAppointment}
            form={riskForm}
            onFormChange={setRiskForm}
            onOpen={openAppointment}
            onSubmit={handleRiskReview}
          />
        ) : null}

        {activeView === 'rules' ? (
          <RulesSection
            busy={busy}
            rules={rules}
            editingRuleId={editingRuleId}
            form={ruleForm}
            onChange={setRuleForm}
            onEdit={editRule}
            onCancelEdit={() => {
              const activeRule = rules.find((rule) => rule.active)
              setEditingRuleId(null)
              setRuleForm(activeRule ? { name: activeRule.name, settings: activeRule.settings } : emptyRuleForm)
            }}
            onSave={() => void saveRule(false)}
            onSaveAndActivate={() => void saveRule(true)}
            onActivate={(id) => void activateRule(id)}
          />
        ) : null}

        {activeView === 'audit' ? (
          <AuditSection
            busy={busy}
            page={auditPage}
            filters={auditFilters}
            onFiltersChange={setAuditFilters}
            onLoad={(page, event) => void loadAuditLogs(page, event)}
          />
        ) : null}

        {selectedAppointment && activeView !== 'risk' ? (
          <AppointmentDrawer
            detail={selectedAppointment}
            onClose={() => setSelectedAppointment(null)}
            onSwitchRisk={() => setActiveView('risk')}
          />
        ) : null}
      </section>
    </main>
  )
}

function StudentsSection({
  busy,
  importResult,
  onFileChange,
  onSubmit,
}: {
  busy: boolean
  importResult: StudentImportResponse | null
  onFileChange: (file: File | null) => void
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void
}) {
  return (
    <section className="content-grid two-columns">
      <form className="panel" onSubmit={onSubmit}>
        <div className="section-heading">
          <h2>Excel 学生导入</h2>
          <p>支持表头：学号、姓名、初始密码、学院、专业、班级、年级、性别、手机号、状态。</p>
        </div>
        <label>
          选择文件
          <input
            type="file"
            accept=".xlsx,.xls"
            onChange={(event) => onFileChange(event.target.files?.[0] || null)}
          />
        </label>
        <button className="primary-button" type="submit" disabled={busy}>
          上传并导入
        </button>
      </form>

      <section className="panel">
        <div className="section-heading">
          <h2>导入结果</h2>
          <p>返回批次、成功数、失败数和逐行处理状态。</p>
        </div>
        {importResult ? (
          <>
            <div className="result-strip">
              <Metric label="总行数" value={importResult.totalRows} />
              <Metric label="成功" value={importResult.successCount} />
              <Metric label="跳过" value={importResult.skippedCount} />
              <Metric label="失败" value={importResult.failedCount} tone={importResult.failedCount ? 'warning' : 'normal'} />
            </div>
            <DataTable
              columns={['行号', '学号', '姓名', '状态', '消息']}
              rows={importResult.rows.slice(0, 12).map((row) => [
                row.rowNo,
                row.studentNo || '-',
                row.name || '-',
                row.status,
                row.message,
              ])}
            />
          </>
        ) : (
          <div className="empty-state">还没有导入结果。</div>
        )}
      </section>
    </section>
  )
}

function CounselorsSection({
  campuses,
  counselors,
  form,
  onChange,
  onSubmit,
}: {
  campuses: Campus[]
  counselors: Counselor[]
  form: typeof emptyCounselorForm
  onChange: (form: typeof emptyCounselorForm) => void
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void
}) {
  return (
    <section className="content-grid">
      <form className="panel form-grid" onSubmit={onSubmit}>
        <div className="section-heading wide">
          <h2>新增咨询师</h2>
          <p>创建账号后，咨询师可在小程序端使用初始密码登录。</p>
        </div>
        <label>
          登录账号
          <input value={form.username} onChange={(event) => onChange({ ...form, username: event.target.value })} required />
        </label>
        <label>
          初始密码
          <input
            value={form.initialPassword}
            onChange={(event) => onChange({ ...form, initialPassword: event.target.value })}
            required
          />
        </label>
        <label>
          姓名
          <input value={form.name} onChange={(event) => onChange({ ...form, name: event.target.value })} required />
        </label>
        <label>
          头衔
          <input value={form.title} onChange={(event) => onChange({ ...form, title: event.target.value })} />
        </label>
        <label>
          校区
          <select value={form.campusId} onChange={(event) => onChange({ ...form, campusId: event.target.value })}>
            <option value="">未指定</option>
            {campuses.map((campus) => (
              <option key={campus.id} value={campus.id}>
                {campus.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          性别
          <select value={form.gender} onChange={(event) => onChange({ ...form, gender: event.target.value })}>
            <option value="FEMALE">FEMALE</option>
            <option value="MALE">MALE</option>
            <option value="OTHER">OTHER</option>
          </select>
        </label>
        <label>
          擅长方向
          <input value={form.expertise} onChange={(event) => onChange({ ...form, expertise: event.target.value })} />
        </label>
        <label>
          每日上限
          <input
            type="number"
            min="0"
            value={form.maxDailyCount}
            onChange={(event) => onChange({ ...form, maxDailyCount: Number(event.target.value) })}
          />
        </label>
        <label className="wide">
          简介
          <textarea value={form.intro} onChange={(event) => onChange({ ...form, intro: event.target.value })} />
        </label>
        <label className="toggle-row">
          <input
            type="checkbox"
            checked={form.visible}
            onChange={(event) => onChange({ ...form, visible: event.target.checked })}
          />
          学生端可见
        </label>
        <button className="primary-button" type="submit">
          创建咨询师
        </button>
      </form>

      <section className="panel">
        <div className="section-heading">
          <h2>咨询师列表</h2>
          <p>用于排班模板、时段生成和预约分配。</p>
        </div>
        <DataTable
          columns={['ID', '姓名', '账号', '校区', '可见', '状态']}
          rows={counselors.map((item) => [
            item.id,
            item.name,
            item.username,
            item.campusName || '-',
            item.visible ? '是' : '否',
            item.status,
          ])}
        />
      </section>
    </section>
  )
}

function SchedulesSection({
  campuses,
  counselors,
  rooms,
  serviceTypes,
  schedules,
  scheduleForm,
  slotForm,
  slotResult,
  onScheduleChange,
  onSlotChange,
  onCreateSchedule,
  onGenerateSlots,
}: {
  campuses: Campus[]
  counselors: Counselor[]
  rooms: Room[]
  serviceTypes: ServiceType[]
  schedules: ScheduleTemplate[]
  scheduleForm: typeof emptyScheduleForm
  slotForm: { startDate: string; endDate: string; counselorId: string }
  slotResult: GenerateSlotsResponse | null
  onScheduleChange: (form: typeof emptyScheduleForm) => void
  onSlotChange: (form: { startDate: string; endDate: string; counselorId: string }) => void
  onCreateSchedule: (event: React.FormEvent<HTMLFormElement>) => void
  onGenerateSlots: (event: React.FormEvent<HTMLFormElement>) => void
}) {
  const filteredRooms = rooms.filter((room) => !scheduleForm.campusId || room.campusId === Number(scheduleForm.campusId))
  return (
    <section className="content-grid">
      <form className="panel form-grid" onSubmit={onCreateSchedule}>
        <div className="section-heading wide">
          <h2>排班模板</h2>
          <p>创建每周固定可咨询时间段，再生成具体可预约时段。</p>
        </div>
        <label>
          咨询师
          <select
            value={scheduleForm.counselorId}
            onChange={(event) => onScheduleChange({ ...scheduleForm, counselorId: event.target.value })}
            required
          >
            <option value="">请选择</option>
            {counselors.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          校区
          <select
            value={scheduleForm.campusId}
            onChange={(event) => onScheduleChange({ ...scheduleForm, campusId: event.target.value, roomId: '' })}
            required
          >
            <option value="">请选择</option>
            {campuses.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          咨询室
          <select value={scheduleForm.roomId} onChange={(event) => onScheduleChange({ ...scheduleForm, roomId: event.target.value })}>
            <option value="">不指定</option>
            {filteredRooms.map((room) => (
              <option key={room.id} value={room.id}>
                {room.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          服务类型
          <select
            value={scheduleForm.serviceTypeId}
            onChange={(event) => onScheduleChange({ ...scheduleForm, serviceTypeId: event.target.value })}
            required
          >
            <option value="">请选择</option>
            {serviceTypes.map((item) => (
              <option key={item.id} value={item.id}>
                {item.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          星期
          <select value={scheduleForm.dayOfWeek} onChange={(event) => onScheduleChange({ ...scheduleForm, dayOfWeek: event.target.value })}>
            {[1, 2, 3, 4, 5, 6, 7].map((day) => (
              <option key={day} value={day}>
                {dayName(day)}
              </option>
            ))}
          </select>
        </label>
        <label>
          开始时间
          <input value={scheduleForm.startTime} onChange={(event) => onScheduleChange({ ...scheduleForm, startTime: event.target.value })} />
        </label>
        <label>
          结束时间
          <input value={scheduleForm.endTime} onChange={(event) => onScheduleChange({ ...scheduleForm, endTime: event.target.value })} />
        </label>
        <label>
          生效日期
          <input
            type="date"
            value={scheduleForm.effectiveFrom}
            onChange={(event) => onScheduleChange({ ...scheduleForm, effectiveFrom: event.target.value })}
          />
        </label>
        <button className="primary-button" type="submit">
          保存排班模板
        </button>
      </form>

      <section className="panel stack">
        <form className="form-grid compact" onSubmit={onGenerateSlots}>
          <div className="section-heading wide">
            <h2>生成可约时段</h2>
            <p>按已有排班模板批量生成具体 slot。</p>
          </div>
          <label>
            开始日期
            <input type="date" value={slotForm.startDate} onChange={(event) => onSlotChange({ ...slotForm, startDate: event.target.value })} />
          </label>
          <label>
            结束日期
            <input type="date" value={slotForm.endDate} onChange={(event) => onSlotChange({ ...slotForm, endDate: event.target.value })} />
          </label>
          <label>
            咨询师
            <select value={slotForm.counselorId} onChange={(event) => onSlotChange({ ...slotForm, counselorId: event.target.value })}>
              <option value="">全部咨询师</option>
              {counselors.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name}
                </option>
              ))}
            </select>
          </label>
          <button className="secondary-button" type="submit">
            生成时段
          </button>
        </form>
        {slotResult ? (
          <div className="result-strip">
            <Metric label="新增" value={slotResult.generatedCount} />
            <Metric label="已有" value={slotResult.existingCount} />
            <Metric label="跳过过去" value={slotResult.skippedPastCount} />
            <Metric label="超上限" value={slotResult.skippedLimitCount} />
          </div>
        ) : null}
        <DataTable
          columns={['咨询师', '星期', '时间', '地点', '服务', '状态']}
          rows={schedules.map((item) => [
            item.counselorName || item.counselorId,
            dayName(item.dayOfWeek),
            `${trimSeconds(item.startTime)}-${trimSeconds(item.endTime)}`,
            `${item.campusName || '-'} ${item.roomName || ''}`,
            item.serviceTypeName || item.serviceTypeId,
            item.status,
          ])}
        />
      </section>
    </section>
  )
}

function AppointmentsSection({
  appointments,
  filters,
  activeCount,
  onFiltersChange,
  onLoad,
  onOpen,
}: {
  appointments: AppointmentRecord[]
  filters: {
    status: string
    riskLevel: string
    studentNo: string
    counselorId: string
    from: string
    to: string
  }
  activeCount: number
  onFiltersChange: (filters: {
    status: string
    riskLevel: string
    studentNo: string
    counselorId: string
    from: string
    to: string
  }) => void
  onLoad: (event?: React.FormEvent<HTMLFormElement>) => void
  onOpen: (id: number) => void
}) {
  return (
    <section className="panel stack">
      <form className="toolbar-form" onSubmit={onLoad}>
        <select value={filters.status} onChange={(event) => onFiltersChange({ ...filters, status: event.target.value })}>
          <option value="">全部状态</option>
          <option value="RISK_REVIEW">风险审核</option>
          <option value="CONFIRMED">已确认</option>
          <option value="COMPLETED">已完成</option>
          <option value="REFERRED">已转介</option>
          <option value="CLOSED">已关闭</option>
        </select>
        <select value={filters.riskLevel} onChange={(event) => onFiltersChange({ ...filters, riskLevel: event.target.value })}>
          <option value="">全部风险</option>
          <option value="LOW">普通</option>
          <option value="MEDIUM">中等</option>
          <option value="HIGH">高风险</option>
        </select>
        <input
          placeholder="学号"
          value={filters.studentNo}
          onChange={(event) => onFiltersChange({ ...filters, studentNo: event.target.value })}
        />
        <input type="date" value={filters.from} onChange={(event) => onFiltersChange({ ...filters, from: event.target.value })} />
        <input type="date" value={filters.to} onChange={(event) => onFiltersChange({ ...filters, to: event.target.value })} />
        <button className="secondary-button" type="submit">
          查询
        </button>
      </form>
      <div className="section-heading inline">
        <h2>预约列表</h2>
        <p>当前待处理 {activeCount} 条，点击行查看详情。</p>
      </div>
      <AppointmentTable appointments={appointments} onOpen={onOpen} />
    </section>
  )
}

function RiskSection({
  appointments,
  selected,
  form,
  onFormChange,
  onOpen,
  onSubmit,
}: {
  appointments: AppointmentRecord[]
  selected: AppointmentDetail | null
  form: {
    decision: string
    handlingNotes: string
    referralType: string
    referralDestination: string
    referralReason: string
  }
  onFormChange: (form: {
    decision: string
    handlingNotes: string
    referralType: string
    referralDestination: string
    referralReason: string
  }) => void
  onOpen: (id: number) => void
  onSubmit: (event: React.FormEvent<HTMLFormElement>) => void
}) {
  return (
    <section className="content-grid two-columns">
      <section className="panel stack">
        <div className="section-heading">
          <h2>高风险队列</h2>
          <p>优先处理 `RISK_REVIEW` 状态预约。</p>
        </div>
        {appointments.length ? (
          <AppointmentTable appointments={appointments} onOpen={onOpen} />
        ) : (
          <div className="empty-state">当前没有高风险或待审核预约。</div>
        )}
      </section>

      <section className="panel stack">
        <div className="section-heading">
          <h2>审核操作</h2>
          <p>选择左侧预约后提交审核结论。</p>
        </div>
        {selected ? (
          <>
            <DetailSummary detail={selected} />
            {selected.status === 'RISK_REVIEW' ? (
              <form className="form-grid compact" onSubmit={onSubmit}>
                <label>
                  审核决定
                  <select value={form.decision} onChange={(event) => onFormChange({ ...form, decision: event.target.value })}>
                    <option value="APPROVE">通过并确认</option>
                    <option value="REFER">转介</option>
                    <option value="CLOSE">关闭</option>
                  </select>
                </label>
                <label className="wide">
                  处理说明
                  <textarea value={form.handlingNotes} onChange={(event) => onFormChange({ ...form, handlingNotes: event.target.value })} />
                </label>
                {form.decision === 'REFER' ? (
                  <>
                    <label>
                      转介类型
                      <select value={form.referralType} onChange={(event) => onFormChange({ ...form, referralType: event.target.value })}>
                        <option value="HOSPITAL">医院</option>
                        <option value="CRISIS_CENTER">危机中心</option>
                        <option value="GUARDIAN">监护人</option>
                      </select>
                    </label>
                    <label>
                      转介去向
                      <input
                        value={form.referralDestination}
                        onChange={(event) => onFormChange({ ...form, referralDestination: event.target.value })}
                      />
                    </label>
                    <label className="wide">
                      转介原因
                      <textarea value={form.referralReason} onChange={(event) => onFormChange({ ...form, referralReason: event.target.value })} />
                    </label>
                  </>
                ) : null}
                <button className="primary-button" type="submit">
                  提交审核
                </button>
              </form>
            ) : (
              <div className="empty-state">该预约当前状态为 {statusText(selected.status)}，无需继续审核。</div>
            )}
          </>
        ) : (
          <div className="empty-state">请选择一条预约查看风险信息。</div>
        )}
      </section>
    </section>
  )
}

function RulesSection({
  busy,
  rules,
  editingRuleId,
  form,
  onChange,
  onEdit,
  onCancelEdit,
  onSave,
  onSaveAndActivate,
  onActivate,
}: {
  busy: boolean
  rules: AppointmentRule[]
  editingRuleId: number | null
  form: typeof emptyRuleForm
  onChange: (form: typeof emptyRuleForm) => void
  onEdit: (rule: AppointmentRule) => void
  onCancelEdit: () => void
  onSave: () => void
  onSaveAndActivate: () => void
  onActivate: (id: number) => void
}) {
  function updateSetting(key: keyof AppointmentRuleSettings, value: number) {
    onChange({ ...form, settings: { ...form.settings, [key]: value } })
  }

  return (
    <section className="content-grid rules-layout">
      <form
        className="panel form-grid"
        onSubmit={(event) => {
          event.preventDefault()
          onSave()
        }}
      >
        <div className="section-heading wide">
          <h2>{editingRuleId ? `编辑规则草稿 #${editingRuleId}` : '新建规则版本'}</h2>
          <p>启用后立即作用于学生可约时段、预约次数、锁定和取消限制。</p>
        </div>
        <label className="wide">
          规则名称
          <input
            value={form.name}
            maxLength={128}
            onChange={(event) => onChange({ ...form, name: event.target.value })}
            required
          />
        </label>
        <RuleNumberInput label="时段间隔（分钟）" value={form.settings.slotGapMinutes} min={0} max={120} onChange={(value) => updateSetting('slotGapMinutes', value)} />
        <RuleNumberInput label="时段锁定（分钟）" value={form.settings.slotLockMinutes} min={1} max={60} onChange={(value) => updateSetting('slotLockMinutes', value)} />
        <RuleNumberInput label="最长提前预约（天）" value={form.settings.maxBookingDaysAhead} min={1} max={60} onChange={(value) => updateSetting('maxBookingDaysAhead', value)} />
        <RuleNumberInput label="最短提前预约（小时）" value={form.settings.minBookingHoursAhead} min={0} max={336} onChange={(value) => updateSetting('minBookingHoursAhead', value)} />
        <RuleNumberInput label="最晚取消提前量（小时）" value={form.settings.minCancelHoursAhead} min={0} max={336} onChange={(value) => updateSetting('minCancelHoursAhead', value)} />
        <RuleNumberInput label="每周预约上限" value={form.settings.maxWeeklyAppointments} min={1} max={10} onChange={(value) => updateSetting('maxWeeklyAppointments', value)} />
        <RuleNumberInput label="每学期完成上限" value={form.settings.maxSemesterCompletedAppointments} min={1} max={100} onChange={(value) => updateSetting('maxSemesterCompletedAppointments', value)} />
        <RuleNumberInput label="同时有效预约上限" value={form.settings.maxActiveAppointments} min={1} max={5} onChange={(value) => updateSetting('maxActiveAppointments', value)} />
        <div className="button-row wide">
          <button className="secondary-button" type="submit" disabled={busy}>保存草稿</button>
          <button className="primary-button" type="button" disabled={busy} onClick={onSaveAndActivate}>保存并启用</button>
          {editingRuleId ? <button className="ghost-button" type="button" onClick={onCancelEdit}>取消编辑</button> : null}
        </div>
      </form>

      <section className="panel stack">
        <div className="section-heading">
          <h2>规则版本</h2>
          <p>已启用版本不可直接修改；调整时创建新版本，历史预约继续保留原规则引用。</p>
        </div>
        {rules.length ? (
          <div className="table-wrap">
            <table className="rules-table">
              <thead>
                <tr><th>版本</th><th>预约窗口</th><th>次数限制</th><th>生效时间</th><th>操作</th></tr>
              </thead>
              <tbody>
                {rules.map((rule) => (
                  <tr key={rule.id}>
                    <td>
                      <strong>{rule.name}</strong>
                      <small>#{rule.id} · {rule.publishedByUsername || rule.publishedBy}</small>
                      <span className={`status-pill ${rule.active ? 'success' : 'neutral'}`}>
                        {rule.active ? '当前启用' : rule.effectiveTo ? '历史版本' : '草稿'}
                      </span>
                    </td>
                    <td>
                      <strong>{rule.settings.minBookingHoursAhead} 小时后至 {rule.settings.maxBookingDaysAhead} 天内</strong>
                      <small>锁定 {rule.settings.slotLockMinutes} 分钟 · 间隔 {rule.settings.slotGapMinutes} 分钟</small>
                    </td>
                    <td>
                      <strong>每周 {rule.settings.maxWeeklyAppointments} 次</strong>
                      <small>有效 {rule.settings.maxActiveAppointments} · 学期完成 {rule.settings.maxSemesterCompletedAppointments}</small>
                    </td>
                    <td>{formatDateTime(rule.effectiveFrom)}</td>
                    <td>
                      <div className="table-actions">
                        <button className="link-button" type="button" onClick={() => onEdit(rule)}>
                          {!rule.active && !rule.effectiveTo ? '编辑' : '基于此新建'}
                        </button>
                        {!rule.active ? <button className="link-button" type="button" onClick={() => onActivate(rule.id)}>启用</button> : null}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <div className="empty-state">暂无规则版本。</div>}
      </section>
    </section>
  )
}

function RuleNumberInput({
  label,
  value,
  min,
  max,
  onChange,
}: {
  label: string
  value: number
  min: number
  max: number
  onChange: (value: number) => void
}) {
  return (
    <label>
      {label}
      <input type="number" value={value} min={min} max={max} onChange={(event) => onChange(Number(event.target.value))} required />
    </label>
  )
}

function AuditSection({
  busy,
  page,
  filters,
  onFiltersChange,
  onLoad,
}: {
  busy: boolean
  page: AuditLogPage
  filters: { action: string; targetType: string; sensitiveLevel: string; actorAccountId: string; from: string; to: string }
  onFiltersChange: (filters: { action: string; targetType: string; sensitiveLevel: string; actorAccountId: string; from: string; to: string }) => void
  onLoad: (page: number, event?: React.FormEvent<HTMLFormElement>) => void
}) {
  return (
    <section className="panel stack">
      <form className="audit-toolbar" onSubmit={(event) => onLoad(0, event)}>
        <select value={filters.action} onChange={(event) => onFiltersChange({ ...filters, action: event.target.value })}>
          <option value="">全部操作</option>
          {auditActionOptions.map((action) => <option key={action} value={action}>{auditActionText(action)}</option>)}
        </select>
        <select value={filters.targetType} onChange={(event) => onFiltersChange({ ...filters, targetType: event.target.value })}>
          <option value="">全部对象</option>
          <option value="APPOINTMENT_RULE_SET">预约规则</option>
          <option value="STUDENT_IMPORT_BATCH">学生导入</option>
          <option value="COUNSELOR">咨询师</option>
          <option value="SCHEDULE_TEMPLATE">排班</option>
          <option value="APPOINTMENT_SLOT">预约时段</option>
          <option value="APPOINTMENT">预约</option>
          <option value="ACCOUNT">账号</option>
        </select>
        <select value={filters.sensitiveLevel} onChange={(event) => onFiltersChange({ ...filters, sensitiveLevel: event.target.value })}>
          <option value="">全部敏感级别</option>
          <option value="NORMAL">普通</option>
          <option value="SENSITIVE">敏感</option>
          <option value="HIGH">高敏感</option>
        </select>
        <input type="number" min="1" placeholder="操作账号 ID" value={filters.actorAccountId} onChange={(event) => onFiltersChange({ ...filters, actorAccountId: event.target.value })} />
        <input type="date" value={filters.from} onChange={(event) => onFiltersChange({ ...filters, from: event.target.value })} />
        <input type="date" value={filters.to} onChange={(event) => onFiltersChange({ ...filters, to: event.target.value })} />
        <button className="secondary-button" type="submit" disabled={busy}>查询</button>
      </form>
      <div className="section-heading inline">
        <h2>关键操作记录</h2>
        <p>共 {page.totalElements} 条，仅记录业务元数据，不保存密码和咨询正文。</p>
      </div>
      {page.items.length ? (
        <div className="table-wrap">
          <table className="audit-table">
            <thead><tr><th>时间</th><th>操作人</th><th>操作</th><th>对象</th><th>级别</th><th>结果摘要</th><th>来源</th></tr></thead>
            <tbody>
              {page.items.map((log) => (
                <tr key={log.id}>
                  <td>{formatDateTime(log.createdAt)}</td>
                  <td><strong>{log.actorUsername || `账号 ${log.actorAccountId}`}</strong><small>ID {log.actorAccountId}</small></td>
                  <td>{auditActionText(log.action)}</td>
                  <td><strong>{targetTypeText(log.targetType)}</strong><small>{log.targetId ? `#${log.targetId}` : '-'}</small></td>
                  <td><span className={`status-pill ${sensitiveTone(log.sensitiveLevel)}`}>{sensitiveText(log.sensitiveLevel)}</span></td>
                  <td className="audit-detail">{formatAuditDetail(log.detail)}</td>
                  <td><strong>{log.ip || '-'}</strong><small title={log.userAgent}>{shortUserAgent(log.userAgent)}</small></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : <div className="empty-state">当前筛选条件下没有审计记录。</div>}
      <div className="pagination-row">
        <button className="ghost-button" type="button" disabled={busy || page.page <= 0} onClick={() => onLoad(page.page - 1)}>上一页</button>
        <span>第 {page.totalPages ? page.page + 1 : 0} / {page.totalPages} 页</span>
        <button className="ghost-button" type="button" disabled={busy || page.page + 1 >= page.totalPages} onClick={() => onLoad(page.page + 1)}>下一页</button>
      </div>
    </section>
  )
}

function AppointmentDrawer({
  detail,
  onClose,
  onSwitchRisk,
}: {
  detail: AppointmentDetail
  onClose: () => void
  onSwitchRisk: () => void
}) {
  return (
    <aside className="drawer">
      <div className="drawer-head">
        <h2>预约详情</h2>
        <button className="ghost-button" type="button" onClick={onClose}>
          关闭
        </button>
      </div>
      <DetailSummary detail={detail} />
      <div className="detail-grid">
        <span>首次咨询</span>
        <strong>{detail.firstVisit ? '是' : '否'}</strong>
        <span>问题类型</span>
        <strong>{detail.issueTypes?.join('、') || '-'}</strong>
        <span>紧急程度</span>
        <strong>{riskText(detail.urgencyLevel)}</strong>
        <span>风险筛查</span>
        <strong>{riskFlags(detail)}</strong>
      </div>
      {detail.status === 'RISK_REVIEW' ? (
        <button className="primary-button" type="button" onClick={onSwitchRisk}>
          去高风险审核
        </button>
      ) : null}
    </aside>
  )
}

function DetailSummary({ detail }: { detail: AppointmentDetail }) {
  return (
    <div className="detail-card">
      <div>
        <h3>{detail.studentName || detail.studentNo}</h3>
        <p>{detail.appointmentNo}</p>
      </div>
      <span className={`status-pill ${pillTone(detail.status, detail.riskLevel)}`}>{statusText(detail.status)}</span>
      <div className="detail-grid">
        <span>学生</span>
        <strong>
          {detail.studentNo} · {detail.college || '-'} · {detail.grade || '-'}
        </strong>
        <span>咨询师</span>
        <strong>{detail.counselorName || '-'}</strong>
        <span>时间</span>
        <strong>{timeRange(detail.startAt, detail.endAt)}</strong>
        <span>地点</span>
        <strong>
          {detail.campusName || '-'} {detail.roomName || ''}
        </strong>
        <span>风险</span>
        <strong>{riskText(detail.riskLevel)}</strong>
      </div>
    </div>
  )
}

function AppointmentTable({
  appointments,
  onOpen,
}: {
  appointments: AppointmentRecord[]
  onOpen: (id: number) => void
}) {
  if (!appointments.length) {
    return <div className="empty-state">没有匹配的预约。</div>
  }
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            <th>预约号</th>
            <th>学生</th>
            <th>咨询师</th>
            <th>时间</th>
            <th>风险</th>
            <th>状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          {appointments.map((item) => (
            <tr key={item.appointmentId}>
              <td>{item.appointmentNo}</td>
              <td>
                <strong>{item.studentName || item.studentNo}</strong>
                <small>{item.studentNo}</small>
              </td>
              <td>{item.counselorName || '-'}</td>
              <td>{timeRange(item.startAt, item.endAt)}</td>
              <td>
                <span className={`status-pill ${pillTone(item.status, item.riskLevel)}`}>{riskText(item.riskLevel)}</span>
              </td>
              <td>{statusText(item.status)}</td>
              <td>
                <button className="link-button" type="button" onClick={() => onOpen(item.appointmentId)}>
                  查看
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function DataTable({ columns, rows }: { columns: string[]; rows: Array<Array<string | number | boolean>> }) {
  if (!rows.length) {
    return <div className="empty-state">暂无数据。</div>
  }
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {columns.map((column) => (
              <th key={column}>{column}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {row.map((cell, cellIndex) => (
                <td key={`${rowIndex}-${cellIndex}`}>{String(cell)}</td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function Metric({ label, value, tone = 'normal' }: { label: string; value: number; tone?: 'normal' | 'warning' }) {
  return (
    <div className={`metric ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function readSession() {
  try {
    const raw = localStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as AdminSession) : null
  } catch {
    return null
  }
}

function splitList(value: string) {
  return value
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function numberOrUndefined(value: string) {
  return value ? Number(value) : undefined
}

function addDays(date: Date, days: number) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

function toDateInput(date: Date) {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

function trimSeconds(value?: string) {
  return value ? value.slice(0, 5) : '-'
}

function timeRange(startAt?: string, endAt?: string) {
  if (!startAt || !endAt) {
    return '-'
  }
  return `${startAt.replace('T', ' ').slice(0, 16)} - ${endAt.slice(11, 16)}`
}

function dayName(day: number) {
  return ['一', '二', '三', '四', '五', '六', '日'][day - 1] ? `周${['一', '二', '三', '四', '五', '六', '日'][day - 1]}` : `周${day}`
}

function statusText(status?: string) {
  const map: Record<string, string> = {
    SUBMITTED: '已提交',
    RISK_REVIEW: '风险审核',
    COUNSELOR_REVIEW: '咨询师审核',
    ADMIN_REVIEW: '后台审核',
    CONFIRMED: '已确认',
    CANCELED_BY_STUDENT: '学生取消',
    CANCELED_BY_COUNSELOR: '咨询师取消',
    CANCELED_BY_ADMIN: '管理员取消',
    CHECKED_IN: '已到访',
    NO_SHOW: '未到访',
    COMPLETED: '已完成',
    REFERRED: '已转介',
    CLOSED: '已关闭',
  }
  return status ? map[status] || status : '-'
}

function riskText(risk?: string) {
  const map: Record<string, string> = {
    LOW: '普通风险',
    MEDIUM: '中等风险',
    HIGH: '高风险',
  }
  return risk ? map[risk] || risk : '-'
}

function pillTone(status?: string, risk?: string) {
  if (risk === 'HIGH' || status === 'RISK_REVIEW') {
    return 'danger'
  }
  if (risk === 'MEDIUM' || status === 'REFERRED') {
    return 'warning'
  }
  if (status === 'COMPLETED' || status === 'CONFIRMED') {
    return 'success'
  }
  return 'neutral'
}

function riskFlags(detail: AppointmentDetail) {
  const flags = [
    detail.selfHarm ? '自伤风险' : '',
    detail.harmOthers ? '伤人风险' : '',
    detail.crisisEvent ? '危机事件' : '',
    detail.psychiatricTreatment ? '精神科治疗史' : '',
    detail.medication ? '药物治疗' : '',
  ].filter(Boolean)
  return flags.length ? flags.join('、') : '未勾选高风险项'
}

const auditActionOptions = [
  'ADMIN_LOGIN',
  'STUDENT_IMPORT',
  'COUNSELOR_CREATED',
  'COUNSELOR_UPDATED',
  'SCHEDULE_CREATED',
  'SCHEDULE_UPDATED',
  'SLOTS_GENERATED',
  'APPOINTMENT_CANCELED',
  'APPOINTMENT_MARKED_NO_SHOW',
  'RISK_REVIEWED',
  'APPOINTMENT_RULE_CREATED',
  'APPOINTMENT_RULE_UPDATED',
  'APPOINTMENT_RULE_ACTIVATED',
]

function auditActionText(action: string) {
  const labels: Record<string, string> = {
    ADMIN_LOGIN: '管理员登录',
    STUDENT_IMPORT: '导入学生账号',
    CAMPUS_CREATED: '新增校区',
    CAMPUS_UPDATED: '更新校区',
    ROOM_CREATED: '新增咨询室',
    ROOM_UPDATED: '更新咨询室',
    COUNSELOR_CREATED: '新增咨询师',
    COUNSELOR_UPDATED: '更新咨询师',
    SCHEDULE_CREATED: '新增排班',
    SCHEDULE_UPDATED: '更新排班',
    SLOTS_GENERATED: '生成预约时段',
    APPOINTMENT_CANCELED: '后台取消预约',
    APPOINTMENT_MARKED_NO_SHOW: '咨询师标记未到访',
    RISK_REVIEWED: '高风险审核',
    APPOINTMENT_RULE_CREATED: '新建预约规则',
    APPOINTMENT_RULE_UPDATED: '更新预约规则',
    APPOINTMENT_RULE_ACTIVATED: '启用预约规则',
  }
  return labels[action] || action
}

function targetTypeText(targetType: string) {
  const labels: Record<string, string> = {
    ACCOUNT: '账号',
    STUDENT_IMPORT_BATCH: '导入批次',
    CAMPUS: '校区',
    ROOM: '咨询室',
    COUNSELOR: '咨询师',
    SCHEDULE_TEMPLATE: '排班模板',
    APPOINTMENT_SLOT: '预约时段',
    APPOINTMENT: '预约',
    APPOINTMENT_RULE_SET: '预约规则',
  }
  return labels[targetType] || targetType
}

function sensitiveText(level: string) {
  return { NORMAL: '普通', SENSITIVE: '敏感', HIGH: '高敏感' }[level] || level
}

function sensitiveTone(level: string) {
  if (level === 'HIGH') return 'danger'
  if (level === 'SENSITIVE') return 'warning'
  return 'neutral'
}

function formatAuditDetail(detail: Record<string, unknown>) {
  const entries = Object.entries(detail)
  if (!entries.length) return '-'
  return entries.map(([key, value]) => `${key}: ${String(value)}`).join(' · ')
}

function shortUserAgent(userAgent?: string) {
  if (!userAgent) return '-'
  return userAgent.length > 36 ? `${userAgent.slice(0, 36)}…` : userAgent
}

function formatDateTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 19) : '-'
}

function errorMessage(caught: unknown) {
  return caught instanceof Error ? caught.message : '操作失败'
}

export default App
