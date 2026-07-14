const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');

const cancelableStatuses = [
  'SUBMITTED',
  'RISK_REVIEW',
  'COUNSELOR_REVIEW',
  'ADMIN_REVIEW',
  'CONFIRMED'
];

Page({
  data: {
    id: null,
    loading: true,
    canceling: false,
    detail: {},
    canCancel: false
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
    this.setData({ loading: true });
    return request({ url: `/api/student/appointments/${this.data.id}` })
      .then((data) => {
        const detail = {
          ...data,
          timeText: format.formatTimeRange(data.startAt, data.endAt),
          statusText: format.statusText(data.status),
          tagType: format.statusTagType(data.status),
          counselorText: data.counselorName || '咨询师',
          placeText: `${data.campusName || '校内咨询'} ${data.roomName || ''}`,
          serviceTypeText: data.serviceTypeName || '-',
          firstVisitText: data.firstVisit ? '是' : '否',
          urgencyText: format.urgencyText(data.urgencyLevel),
          contactTimeText: data.contactTime || '-',
          hasIssueTypes: Boolean(data.issueTypes && data.issueTypes.length),
          riskLevelText: format.riskLevelText(data.riskLevel)
        };
        this.setData({
          detail,
          canCancel: cancelableStatuses.includes(data.status)
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  cancelAppointment() {
    wx.showModal({
      title: '取消预约',
      editable: true,
      placeholderText: '请输入取消原因',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        const reason = (res.content || '').trim() || '学生主动取消';
        this.setData({ canceling: true });
        request({
          url: `/api/student/appointments/${this.data.id}/cancel`,
          method: 'POST',
          data: { reason }
        })
          .then(() => {
            wx.showToast({ title: '已取消', icon: 'success' });
            this.loadDetail();
          })
          .finally(() => {
            this.setData({ canceling: false });
          });
      }
    });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  }
});
