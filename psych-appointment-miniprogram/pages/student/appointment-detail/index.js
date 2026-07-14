const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');

const ISSUE_LABELS = {
  'academic-stress': '学业压力',
  'study-pressure': '学业压力',
  emotion: '情绪困扰',
  relationship: '人际关系',
  family: '家庭议题',
  sleep: '睡眠问题',
  career: '生涯发展'
};

const CONTACT_TIME_LABELS = {
  'weekday afternoon': '工作日下午',
  'weekday-afternoon': '工作日下午',
  'workday-afternoon': '工作日下午',
  'weekday morning': '工作日上午',
  'weekday-morning': '工作日上午',
  evening: '傍晚或晚间'
};

const CANCELABLE_STATUSES = [
  'SUBMITTED',
  'RISK_REVIEW',
  'COUNSELOR_REVIEW',
  'ADMIN_REVIEW',
  'CONFIRMED'
];

const CANCELED_STATUSES = [
  'CANCELED_BY_STUDENT',
  'CANCELED_BY_COUNSELOR',
  'CANCELED_BY_ADMIN'
];

const CANCEL_REASON_OPTIONS = [
  { label: '时间安排有变化', value: 'SCHEDULE_CHANGED' },
  { label: '身体不适或临时有事', value: 'TEMPORARY_ISSUE' },
  { label: '希望重新选择咨询师', value: 'CHANGE_COUNSELOR' },
  { label: '其他原因', value: 'OTHER' }
];

const WEEKDAY_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function parseDateTime(value) {
  if (!value) {
    return null;
  }
  const normalized = `${value}`.replace('T', ' ').split('.')[0];
  const parts = normalized.split(' ');
  const dateParts = (parts[0] || '').split('-').map(Number);
  const timeParts = (parts[1] || '00:00:00').split(':').map(Number);
  const date = new Date(
    dateParts[0],
    (dateParts[1] || 1) - 1,
    dateParts[2] || 1,
    timeParts[0] || 0,
    timeParts[1] || 0,
    timeParts[2] || 0
  );
  return Number.isNaN(date.getTime()) ? null : date;
}

function appointmentDateParts(startAt, endAt) {
  const start = parseDateTime(startAt);
  const end = parseDateTime(endAt);
  if (!start) {
    return {
      dayText: '--',
      monthWeekText: '时间待定',
      dateText: '时间待定',
      clockText: '--:--'
    };
  }
  const endText = end ? `${pad(end.getHours())}:${pad(end.getMinutes())}` : '';
  return {
    dayText: pad(start.getDate()),
    monthWeekText: WEEKDAY_LABELS[start.getDay()],
    dateText: `${start.getFullYear()}年${pad(start.getMonth() + 1)}月${pad(start.getDate())}日`,
    clockText: `${pad(start.getHours())}:${pad(start.getMinutes())}${endText ? ` - ${endText}` : ''}`
  };
}

function statusPresentation(status) {
  const presentations = {
    SUBMITTED: ['预约申请已提交', '心理中心正在处理你的预约信息，请留意后续状态。', 'neutral', 'waiting'],
    RISK_REVIEW: ['预约正在优先审核', '心理中心会优先处理并在必要时联系你，请保持联系方式畅通。', 'warning', 'waiting'],
    COUNSELOR_REVIEW: ['预约正在审核', '咨询师正在确认本次安排，审核完成后状态会及时更新。', 'warning', 'waiting'],
    ADMIN_REVIEW: ['预约正在审核', '心理中心正在确认本次安排，请留意后续状态。', 'warning', 'waiting'],
    CONFIRMED: ['预约已确认', '请按预约时间到达，如需调整请在截止时间前处理。', '', 'success'],
    CHECKED_IN: ['已完成到访', '你已完成本次预约签到，请按现场安排等候。', '', 'success'],
    COMPLETED: ['本次咨询已完成', '本次咨询记录已完成，可在“我的预约”中继续查看。', '', 'success'],
    CANCELED_BY_STUDENT: ['预约已取消', '你已取消本次预约，原时段将重新开放。', 'neutral', 'info'],
    CANCELED_BY_COUNSELOR: ['预约已由咨询师取消', '心理中心会根据实际情况协助你重新安排。', 'neutral', 'info'],
    CANCELED_BY_ADMIN: ['预约已由中心取消', '如需继续咨询，请返回预约首页重新选择时段。', 'neutral', 'info'],
    NO_SHOW: ['本次预约已结束', '系统记录本次预约未到访，如有疑问请联系心理中心。', 'neutral', 'info'],
    REFERRED: ['已安排进一步支持', '心理中心正在为你衔接更合适的支持方式。', 'warning', 'info'],
    CLOSED: ['本次预约已关闭', '本次预约流程已结束，如仍需帮助可重新发起预约。', 'neutral', 'info']
  };
  const value = presentations[status] || ['预约状态已更新', '请留意本页状态和心理中心的后续通知。', 'neutral', 'info'];
  return {
    statusTitle: value[0],
    statusDesc: value[1],
    statusClass: value[2],
    statusIconType: value[3]
  };
}

function reasonOptions(selectedValue) {
  return CANCEL_REASON_OPTIONS.map((item) => ({
    ...item,
    activeClass: item.value === selectedValue ? 'active' : ''
  }));
}

Page({
  data: {
    id: null,
    loading: true,
    loadError: false,
    canceling: false,
    detail: {},
    canCancel: false,
    cancelDialogVisible: false,
    cancelReasons: reasonOptions(''),
    selectedCancelReason: '',
    cancelReasonDetail: '',
    cancelReasonLength: 0,
    canConfirmCancel: false
  },

  onLoad(options) {
    this.setData({ id: options.id });
  },

  onShow() {
    if (!auth.ensureLogin()) {
      return;
    }
    this.loadDetail();
  },

  loadDetail() {
    this.setData({ loading: true, loadError: false });
    return request({ url: `/api/student/appointments/${this.data.id}` })
      .then((data) => {
        const dateParts = appointmentDateParts(data.startAt, data.endAt);
        const appointmentEndAt = parseDateTime(data.endAt);
        const isPastConfirmed = data.status === 'CONFIRMED'
          && Boolean(appointmentEndAt && appointmentEndAt.getTime() < Date.now());
        const presentation = isPastConfirmed
          ? {
              statusTitle: '预约时间已过',
              statusDesc: '本次预约时间已经结束，最终服务状态待心理中心更新。',
              statusClass: 'neutral',
              statusIconType: 'info'
            }
          : statusPresentation(data.status);
        const fallbackCanCancel = CANCELABLE_STATUSES.includes(data.status)
          && Boolean(parseDateTime(data.startAt) && parseDateTime(data.startAt).getTime() > Date.now());
        const canCancel = typeof data.canCancel === 'boolean' ? data.canCancel : fallbackCanCancel;
        const minCancelHoursAhead = Number.isInteger(data.minCancelHoursAhead)
          ? data.minCancelHoursAhead
          : 24;
        const cancelDeadlineText = data.cancelDeadline
          ? format.formatDateTime(data.cancelDeadline)
          : '';
        const isCanceled = CANCELED_STATUSES.includes(data.status);
        let cancellationHint = `请至少在预约开始前 ${minCancelHoursAhead} 小时取消。`;
        if (canCancel && cancelDeadlineText) {
          cancellationHint = `可在 ${cancelDeadlineText} 前在线取消，取消后该时段会重新开放。`;
        } else if (isCanceled) {
          cancellationHint = data.canceledAt
            ? `本次预约已于 ${format.formatDateTime(data.canceledAt)} 取消。`
            : '本次预约已经取消。';
        } else if (isPastConfirmed) {
          cancellationHint = '预约时间已过，不能在线取消。如状态长时间未更新，请联系心理中心。';
        } else if (CANCELABLE_STATUSES.includes(data.status)) {
          cancellationHint = `已进入预约开始前 ${minCancelHoursAhead} 小时，线上取消通道已关闭。如遇特殊情况，请联系心理中心。`;
        } else if (data.status === 'COMPLETED' || data.status === 'CHECKED_IN') {
          cancellationHint = '当前状态已不能在线取消，如有疑问请联系心理中心。';
        }
        const detail = {
          ...data,
          issueTypes: (data.issueTypes || []).map((value) => ISSUE_LABELS[value] || value),
          ...dateParts,
          ...presentation,
          timeText: format.formatTimeRange(data.startAt, data.endAt),
          statusText: isPastConfirmed ? '待更新' : format.statusText(data.status),
          tagType: isPastConfirmed ? 'neutral' : format.statusTagType(data.status),
          counselorText: data.counselorName || '咨询师',
          counselorInitial: `${data.counselorName || '咨询'}`.slice(0, 1),
          counselorTitleText: data.counselorTitle || '心理咨询师',
          placeText: `${data.campusName || '校内咨询'} ${data.roomName || ''}`.trim(),
          serviceTypeText: data.serviceTypeName || '-',
          firstVisitText: data.firstVisit ? '是' : '否',
          urgencyText: format.urgencyText(data.urgencyLevel),
          contactTimeText: CONTACT_TIME_LABELS[data.contactTime] || data.contactTime || '-',
          hasIssueTypes: Boolean(data.issueTypes && data.issueTypes.length),
          riskLevelText: format.riskLevelText(data.riskLevel),
          isCanceled,
          showRebookAction: isCanceled && Boolean(data.counselorId),
          showRiskNotice: data.status === 'RISK_REVIEW',
          canceledAtText: data.canceledAt ? format.formatDateTime(data.canceledAt) : '',
          cancelDeadlineText,
          minCancelHoursAhead,
          cancellationHint
        };
        this.setData({
          detail,
          canCancel
        });
      })
      .catch(() => {
        this.setData({ loadError: true });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  retryLoad() {
    this.loadDetail();
  },

  openCancelDialog() {
    if (!this.data.canCancel || this.data.canceling) {
      return;
    }
    this.setData({
      cancelDialogVisible: true,
      selectedCancelReason: '',
      cancelReasonDetail: '',
      cancelReasonLength: 0,
      canConfirmCancel: false,
      cancelReasons: reasonOptions('')
    });
  },

  closeCancelDialog() {
    if (this.data.canceling) {
      return;
    }
    this.setData({ cancelDialogVisible: false });
  },

  selectCancelReason(event) {
    const selectedCancelReason = event.detail.value || event.currentTarget.dataset.value;
    const canConfirmCancel = selectedCancelReason !== 'OTHER'
      || Boolean(this.data.cancelReasonDetail.trim());
    this.setData({
      selectedCancelReason,
      canConfirmCancel,
      cancelReasons: reasonOptions(selectedCancelReason)
    });
  },

  onCancelReasonInput(event) {
    const cancelReasonDetail = (event.detail.value || '').slice(0, 200);
    const canConfirmCancel = Boolean(this.data.selectedCancelReason)
      && (this.data.selectedCancelReason !== 'OTHER' || Boolean(cancelReasonDetail.trim()));
    this.setData({
      cancelReasonDetail,
      cancelReasonLength: cancelReasonDetail.length,
      canConfirmCancel
    });
  },

  confirmCancellation() {
    if (!this.data.canConfirmCancel || this.data.canceling) {
      wx.showToast({ title: '请选择取消原因', icon: 'none' });
      return Promise.resolve();
    }
    const selected = CANCEL_REASON_OPTIONS.find(
      (item) => item.value === this.data.selectedCancelReason
    );
    const detail = this.data.cancelReasonDetail.trim();
    const reason = this.data.selectedCancelReason === 'OTHER'
      ? detail
      : `${selected ? selected.label : '学生主动取消'}${detail ? `：${detail}` : ''}`;

    this.setData({ canceling: true });
    return request({
      url: `/api/student/appointments/${this.data.id}/cancel`,
      method: 'POST',
      data: { reason }
    })
      .then(() => {
        this.setData({ cancelDialogVisible: false });
        wx.showToast({ title: '预约已取消', icon: 'success' });
        return this.loadDetail();
      })
      .finally(() => {
        this.setData({ canceling: false });
      });
  },

  preventBubble() {},

  copyAppointmentNo() {
    if (!this.data.detail.appointmentNo) {
      return;
    }
    wx.setClipboardData({
      data: this.data.detail.appointmentNo,
      success: () => wx.showToast({ title: '预约号已复制', icon: 'success' })
    });
  },

  rebookCounselor() {
    const counselorId = this.data.detail.counselorId;
    if (!counselorId) {
      this.returnToCounselors();
      return;
    }
    wx.navigateTo({ url: `/pages/student/counselor-detail/index?id=${counselorId}` });
  },

  returnToCounselors() {
    wx.switchTab({ url: '/pages/student/counselors/index' });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  }
});
