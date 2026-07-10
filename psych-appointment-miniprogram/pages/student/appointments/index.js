const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');

Page({
  data: {
    loading: true,
    activeStatus: '',
    appointments: [],
    currentAppointments: [],
    historyAppointments: [],
    filters: [
      { label: '全部', value: '', activeClass: 'active' },
      { label: '已确认', value: 'CONFIRMED', activeClass: '' },
      { label: '风险审核', value: 'RISK_REVIEW', activeClass: '' },
      { label: '已完成', value: 'COMPLETED', activeClass: '' },
      { label: '已取消', value: 'CANCELED_BY_STUDENT', activeClass: '' }
    ]
  },

  onShow() {
    if (!auth.ensureLogin()) {
      return;
    }
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
    const status = this.data.activeStatus;
    const query = status ? `?status=${status}` : '';
    return request({ url: `/api/student/appointments${query}` })
      .then((data) => {
        const appointments = (data || []).map((item) => ({
            ...item,
            timeText: format.formatTimeRange(item.startAt, item.endAt),
            statusText: format.statusText(item.status),
            tagType: format.statusTagType(item.status),
            counselorText: item.counselorName || '咨询师',
            placeText: `${item.campusName || '校内咨询'} ${item.roomName || ''}`,
            isCurrent: ['CONFIRMED', 'RISK_REVIEW', 'COUNSELOR_REVIEW', 'ADMIN_REVIEW'].includes(item.status)
          }));
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
    wx.navigateTo({ url: `/pages/student/appointment-detail/index?id=${event.currentTarget.dataset.id}` });
  }
});
