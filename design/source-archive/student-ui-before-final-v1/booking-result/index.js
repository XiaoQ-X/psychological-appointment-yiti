const format = require('../../../utils/format');
const request = require('../../../utils/request');
const auth = require('../../../utils/auth');

Page({
  data: {
    appointmentId: '',
    status: '',
    counselorName: '',
    slotTime: '',
    placeText: '',
    appointmentNo: '',
    isPendingReview: false,
    statusTitle: '',
    statusDesc: '',
    statusTag: '',
    panelClass: '',
    resultIconType: 'success',
    statusEyebrow: '',
    nextStepTitle: '',
    nextStepDesc: '',
    resultStatusText: '',
    cancelDeadlineText: '请至少在预约开始前 24 小时取消'
  },

  onLoad(options) {
    const status = options.status || 'CONFIRMED';
    const isPendingReview = status === 'RISK_REVIEW';
    this.setData({
      appointmentId: options.id || '',
      status,
      counselorName: decodeURIComponent(options.name || '咨询师'),
      slotTime: decodeURIComponent(options.time || ''),
      isPendingReview,
      statusTitle: isPendingReview ? '信息已提交，等待中心审核' : '预约已确认',
      statusDesc: isPendingReview
        ? '心理中心将尽快审核并在必要时联系你。当前情况紧急时，请立即使用紧急求助渠道。'
        : '预约信息已保存，请确认下方时间和咨询师信息。',
      statusTag: format.statusTagType(status),
      panelClass: isPendingReview ? 'warning' : '',
      resultIconType: isPendingReview ? 'waiting' : 'success',
      statusEyebrow: isPendingReview ? '预约申请已收到' : '预约完成',
      nextStepTitle: isPendingReview ? '留意中心联系' : '按预约时间到访',
      nextStepDesc: isPendingReview
        ? '中心会尽快完成审核，请保持预留联系方式畅通。'
        : '建议提前 10 分钟到达预约地点。',
      resultStatusText: isPendingReview ? '待审核' : '已确认'
    });
    this.loadAppointment();
  },

  onShow() {
    auth.ensureLogin();
  },

  loadAppointment() {
    if (!this.data.appointmentId) {
      return Promise.resolve();
    }
    return request({ url: `/api/student/appointments/${this.data.appointmentId}` })
      .then((data) => {
        const placeText = `${data.campusName || ''} ${data.roomName || ''}`.trim();
        const cancelDeadlineText = data.cancelDeadline
          ? `可在 ${format.formatDateTime(data.cancelDeadline)} 前在线取消`
          : this.data.cancelDeadlineText;
        this.setData({
          counselorName: data.counselorName || this.data.counselorName,
          slotTime: data.startAt
            ? format.formatTimeRange(data.startAt, data.endAt)
            : this.data.slotTime,
          placeText,
          appointmentNo: data.appointmentNo || '',
          cancelDeadlineText
        });
      })
      .catch(() => Promise.resolve());
  },

  copyAppointmentNo() {
    if (!this.data.appointmentNo) {
      return;
    }
    wx.setClipboardData({
      data: this.data.appointmentNo,
      success: () => wx.showToast({ title: '预约号已复制', icon: 'success' })
    });
  },

  openDetail() {
    if (!this.data.appointmentId) {
      wx.switchTab({ url: '/pages/student/appointments/index' });
      return;
    }
    wx.redirectTo({ url: `/pages/student/appointment-detail/index?id=${this.data.appointmentId}` });
  },

  openAppointments() {
    wx.switchTab({ url: '/pages/student/appointments/index' });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  }
});
