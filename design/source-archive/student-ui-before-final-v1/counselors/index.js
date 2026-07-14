const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');
const config = require('../../../config/index');

Page({
  data: {
    loading: true,
    allCounselors: [],
    counselors: [],
    activeFilter: 'ALL',
    nextAppointment: null,
    filterChips: [
      { label: '全部咨询师', value: 'ALL', activeClass: 'active' },
      { label: '压力与情绪', value: 'STRESS', activeClass: '' },
      { label: '本周可约', value: 'WEEK', activeClass: '' }
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
        const counselors = (data || [])
          .map((item) => ({
            ...item,
            expertise: item.expertise || [],
            avatarUrl: item.avatarUrl || '',
            titleText: item.title || '心理咨询师',
            campusText: item.campusName || '校内咨询',
            hasAvailableSlots: item.availableSlotCount > 0,
            availabilityText: item.availableSlotCount > 0
              ? `可约 ${item.availableSlotCount} 个时段`
              : '暂不可约',
            nextAvailableText: item.nextAvailableAt ? format.formatDateTime(item.nextAvailableAt) : ''
          }))
          .sort((left, right) => {
            if (left.hasAvailableSlots !== right.hasAvailableSlots) {
              return left.hasAvailableSlots ? -1 : 1;
            }
            return `${left.nextAvailableAt || '9999'}`.localeCompare(`${right.nextAvailableAt || '9999'}`);
          });
        const activeStatuses = ['CONFIRMED', 'RISK_REVIEW', 'COUNSELOR_REVIEW', 'ADMIN_REVIEW'];
        const next = (appointments || []).find((item) => activeStatuses.includes(item.status));
        this.setData({
          allCounselors: counselors,
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

  changeFilter(event) {
    const value = event.currentTarget.dataset.value;
    const now = new Date();
    const weekEnd = format.addDays(now, 7);
    const stressKeywords = ['压力', '情绪', '学业', '适应', '焦虑'];
    const counselors = this.data.allCounselors.filter((item) => {
      if (value === 'WEEK') {
        if (!item.nextAvailableAt) {
          return false;
        }
        const availableAt = new Date(`${item.nextAvailableAt}`.replace(' ', 'T'));
        return !Number.isNaN(availableAt.getTime()) && availableAt <= weekEnd;
      }
      if (value === 'STRESS') {
        return item.expertise.some((label) => stressKeywords.some((keyword) => `${label}`.includes(keyword)));
      }
      return true;
    });
    this.setData({
      activeFilter: value,
      counselors,
      filterChips: this.data.filterChips.map((item) => ({
        ...item,
        activeClass: item.value === value ? 'active' : ''
      }))
    });
  },

  resetFilter() {
    this.setData({
      activeFilter: 'ALL',
      counselors: this.data.allCounselors,
      filterChips: this.data.filterChips.map((item) => ({
        ...item,
        activeClass: item.value === 'ALL' ? 'active' : ''
      }))
    });
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/student/counselor-detail/index?id=${id}` });
  },

  openCounselorPicker() {
    wx.pageScrollTo({ selector: '#counselor-list', duration: 260 });
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
