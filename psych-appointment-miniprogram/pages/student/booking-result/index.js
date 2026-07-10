const format = require('../../../utils/format');

Page({
  data: {
    appointmentId: '',
    status: '',
    counselorName: '',
    slotTime: '',
    isPendingReview: false,
    statusTitle: '',
    statusDesc: '',
    statusTag: '',
    panelClass: '',
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
        ? '心理中心将优先处理你的信息，必要时会联系你。若当前情况紧急，请立即联系线下人员或拨打紧急电话。'
        : '请按时到访。若需要取消，请在规则允许时间内提前处理。',
      statusTag: format.statusTagType(status),
      panelClass: isPendingReview ? 'warning' : '',
      resultStatusText: isPendingReview ? '待审核' : '已确认'
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
