const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');

const WEEKDAY_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
const CURRENT_STATUSES = [
  'SUBMITTED',
  'RISK_REVIEW',
  'COUNSELOR_REVIEW',
  'ADMIN_REVIEW',
  'CONFIRMED',
  'CHECKED_IN'
];

function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function parseDateTime(value) {
  if (!value) {
    return null;
  }
  if (value instanceof Date) {
    return value;
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
      monthDayText: '--.--',
      weekdayText: '待定',
      clockText: '时间待定'
    };
  }
  const endText = end ? `${pad(end.getHours())}:${pad(end.getMinutes())}` : '';
  return {
    monthDayText: `${pad(start.getMonth() + 1)}.${pad(start.getDate())}`,
    weekdayText: WEEKDAY_LABELS[start.getDay()],
    clockText: `${pad(start.getHours())}:${pad(start.getMinutes())}${endText ? `-${endText}` : ''}`
  };
}

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
        const appointments = (data || []).map((item) => {
          const dateParts = appointmentDateParts(item.startAt, item.endAt);
          return {
            ...item,
            ...dateParts,
            timeText: format.formatTimeRange(item.startAt, item.endAt),
            statusText: format.statusText(item.status),
            tagType: format.statusTagType(item.status),
            counselorText: item.counselorName || '咨询师',
            placeText: `${item.campusName || '校内咨询'} ${item.roomName || ''}`.trim(),
            isCurrent: CURRENT_STATUSES.includes(item.status)
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
    wx.navigateTo({ url: `/pages/student/appointment-detail/index?id=${event.currentTarget.dataset.id}` });
  },

  startBooking() {
    wx.switchTab({ url: '/pages/student/counselors/index' });
  }
});
