const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');

const currentStatuses = ['CONFIRMED', 'CHECKED_IN', 'RISK_REVIEW', 'COUNSELOR_REVIEW', 'ADMIN_REVIEW'];

Page({
  data: {
    loading: true,
    activeStatus: '',
    counselor: null,
    appointments: [],
    currentAppointments: [],
    historyAppointments: [],
    filters: [
      { label: '全部', value: '', activeClass: 'active' },
      { label: '已确认', value: 'CONFIRMED', activeClass: '' },
      { label: '已到访', value: 'CHECKED_IN', activeClass: '' },
      { label: '风险审核', value: 'RISK_REVIEW', activeClass: '' },
      { label: '已完成', value: 'COMPLETED', activeClass: '' },
      { label: '未到访', value: 'NO_SHOW', activeClass: '' }
    ]
  },

  onShow() {
    if (!auth.ensureCounselorLogin()) {
      return;
    }
    this.setData({ counselor: auth.getCounselor() });
    this.loadAppointments();
  },

  onPullDownRefresh() {
    this.loadAppointments().finally(() => wx.stopPullDownRefresh());
  },

  changeFilter(event) {
    const status = event.currentTarget.dataset.value;
    this.setData({
      activeStatus: status,
      filters: this.data.filters.map((item) => ({
        ...item,
        activeClass: item.value === status ? 'active' : ''
      }))
    });
    this.loadAppointments();
  },

  loadAppointments() {
    this.setData({ loading: true });
    const query = this.data.activeStatus ? `?status=${this.data.activeStatus}` : '';
    return request({ url: `/api/counselor/appointments${query}` })
      .then((data) => {
        const appointments = (data || []).map((item) => {
          const startAt = format.parseDateTime(item.startAt).getTime();
          const canComplete = (item.status === 'CONFIRMED' || item.status === 'CHECKED_IN')
            && startAt <= Date.now();
          return {
            ...item,
            timeText: format.formatTimeRange(item.startAt, item.endAt),
            statusText: format.statusText(item.status),
            tagType: format.statusTagType(item.status),
            riskText: format.riskLevelText(item.riskLevel),
            riskTagType: item.riskLevel === 'HIGH' ? 'danger' : item.riskLevel === 'MEDIUM' ? 'warning' : 'neutral',
            studentText: item.studentName || item.studentNo || '学生',
            studentMeta: [item.studentNo, item.college, item.grade, item.className].filter(Boolean).join(' · '),
            placeText: `${item.campusName || '校内咨询'} ${item.roomName || ''}`,
            serviceTypeText: item.serviceTypeName || '心理咨询',
            isCurrent: currentStatuses.includes(item.status),
            canComplete
          };
        });
        this.setData({
          appointments,
          currentAppointments: appointments.filter((item) => item.isCurrent),
          historyAppointments: appointments.filter((item) => !item.isCurrent)
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  openDetail(event) {
    wx.navigateTo({ url: `/pages/counselor/appointment-detail/index?id=${event.currentTarget.dataset.id}` });
  },

  logout() {
    auth.clearSession();
    getApp().globalData.counselor = null;
    wx.reLaunch({ url: '/pages/counselor/login/index' });
  }
});
