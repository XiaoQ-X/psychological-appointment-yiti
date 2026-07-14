const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');

const completeStatuses = ['CONFIRMED', 'CHECKED_IN'];

Page({
  data: {
    id: null,
    loading: true,
    detail: {},
    canComplete: false,
    canMarkNoShow: false,
    markingNoShow: false
  },

  onLoad(options) {
    this.setData({ id: options.id });
  },

  onShow() {
    if (!auth.ensureCounselorLogin()) {
      return;
    }
    this.loadDetail();
  },

  onPullDownRefresh() {
    this.loadDetail().finally(() => wx.stopPullDownRefresh());
  },

  loadDetail() {
    this.setData({ loading: true });
    return request({ url: `/api/counselor/appointments/${this.data.id}` })
      .then((data) => {
        const now = Date.now();
        const startAt = format.parseDateTime(data.startAt).getTime();
        const endAt = format.parseDateTime(data.endAt).getTime();
        const issueTypes = (data.issueTypes || []).map(format.issueTypeText);
        const hasIssueTypes = Boolean(issueTypes.length);
        const detail = {
          ...data,
          timeText: format.formatTimeRange(data.startAt, data.endAt),
          statusText: format.statusText(data.status),
          tagType: format.statusTagType(data.status),
          riskLevelText: format.riskLevelText(data.riskLevel),
          riskTagType: data.riskLevel === 'HIGH' ? 'danger' : data.riskLevel === 'MEDIUM' ? 'warning' : 'neutral',
          urgencyText: format.urgencyText(data.urgencyLevel),
          firstVisitText: data.firstVisit ? '是' : '否',
          contactTimeText: format.contactTimeText(data.contactTime),
          issueTypes,
          studentText: data.studentName || data.studentNo || '学生',
          studentNoText: data.studentNo || '-',
          studentProfileText: [data.gender, data.college, data.major, data.grade, data.className].filter(Boolean).join(' · ') || '-',
          placeText: `${data.campusName || '校内咨询'} ${data.roomName || ''}`,
          serviceTypeText: data.serviceTypeName || '心理咨询',
          hasIssueTypes
        };
        this.setData({
          detail,
          canComplete: completeStatuses.includes(data.status) && startAt <= now,
          canMarkNoShow: data.status === 'CONFIRMED' && endAt <= now
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  openCompleteForm() {
    wx.navigateTo({ url: `/pages/counselor/complete-form/index?id=${this.data.id}` });
  },

  markNoShow() {
    if (!this.data.canMarkNoShow || this.data.markingNoShow) {
      return;
    }
    wx.showModal({
      title: '确认学生未到访',
      content: '确认后，本次预约将记为未到访并计入学生爽约次数。此操作应以实际签到情况为准。',
      confirmText: '确认未到',
      confirmColor: '#d14343',
      success: (result) => {
        if (!result.confirm) {
          return;
        }
        this.setData({ markingNoShow: true });
        request({
          url: `/api/counselor/appointments/${this.data.id}/no-show`,
          method: 'POST'
        })
          .then(() => {
            wx.showToast({ title: '已标记未到访', icon: 'success' });
            return this.loadDetail();
          })
          .finally(() => {
            this.setData({ markingNoShow: false });
          });
      }
    });
  }
});
