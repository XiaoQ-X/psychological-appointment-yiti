const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');
const config = require('../../../config/index');

Page({
  data: {
    loading: true,
    counselors: [],
    nextAppointment: null,
    filterChips: [
      { label: '全部校区', activeClass: 'active' },
      { label: '情绪压力', activeClass: '' },
      { label: '本周可约', activeClass: '' }
    ]
  },

  onShow() {
    if (!auth.ensureLogin()) {
      return;
    }
    this.loadCounselors();
  },

  onPullDownRefresh() {
    this.loadCounselors().finally(() => wx.stopPullDownRefresh());
  },

  loadCounselors() {
    this.setData({ loading: true });
    const from = format.toDateString(new Date());
    const to = format.toDateString(format.addDays(new Date(), config.defaultLookAheadDays));
    return Promise.all([
      request({ url: `/api/student/counselors?from=${from}&to=${to}` }),
      request({ url: '/api/student/appointments' }).catch(() => [])
    ])
      .then(([data, appointments]) => {
        const counselors = (data || []).map((item) => ({
          ...item,
          expertise: item.expertise || [],
          titleText: item.title || '心理咨询师',
          campusText: item.campusName || '校内咨询',
          slotTagType: item.availableSlotCount > 0 ? '' : 'neutral',
          nextAvailableText: item.nextAvailableAt ? format.formatDateTime(item.nextAvailableAt) : ''
        }));
        const activeStatuses = ['CONFIRMED', 'RISK_REVIEW', 'COUNSELOR_REVIEW', 'ADMIN_REVIEW'];
        const next = (appointments || []).find((item) => activeStatuses.includes(item.status));
        this.setData({
          counselors,
          nextAppointment: next ? {
            ...next,
            timeText: format.formatTimeRange(next.startAt, next.endAt),
            statusText: format.statusText(next.status),
            tagType: format.statusTagType(next.status),
            counselorText: next.counselorName || '咨询师',
            placeText: `${next.campusName || '校内咨询'} ${next.roomName || ''}`,
            id: next.appointmentId
          } : null
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/student/counselor-detail/index?id=${id}` });
  },

  openNextAppointment() {
    if (this.data.nextAppointment && this.data.nextAppointment.id) {
      wx.navigateTo({ url: `/pages/student/appointment-detail/index?id=${this.data.nextAppointment.id}` });
      return;
    }
    wx.switchTab({ url: '/pages/student/appointments/index' });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  },

  openConsent() {
    wx.navigateTo({ url: '/pages/student/consent/index' });
  },

  logout() {
    auth.clearSession();
    wx.reLaunch({ url: '/pages/login/login' });
  }
});
