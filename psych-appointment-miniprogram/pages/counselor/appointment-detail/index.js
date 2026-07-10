const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');

const completeStatuses = ['CONFIRMED', 'CHECKED_IN'];

Page({
  data: {
    id: null,
    loading: true,
    detail: {},
    canComplete: false
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
        const hasIssueTypes = Boolean(data.issueTypes && data.issueTypes.length);
        const detail = {
          ...data,
          timeText: format.formatTimeRange(data.startAt, data.endAt),
          statusText: format.statusText(data.status),
          tagType: format.statusTagType(data.status),
          riskLevelText: format.riskLevelText(data.riskLevel),
          riskTagType: data.riskLevel === 'HIGH' ? 'danger' : data.riskLevel === 'MEDIUM' ? 'warning' : 'neutral',
          urgencyText: format.urgencyText(data.urgencyLevel),
          firstVisitText: data.firstVisit ? '是' : '否',
          contactTimeText: data.contactTime || '-',
          studentText: data.studentName || data.studentNo || '学生',
          studentNoText: data.studentNo || '-',
          studentProfileText: [data.gender, data.college, data.major, data.grade, data.className].filter(Boolean).join(' · ') || '-',
          placeText: `${data.campusName || '校内咨询'} ${data.roomName || ''}`,
          serviceTypeText: data.serviceTypeName || '心理咨询',
          hasIssueTypes
        };
        this.setData({
          detail,
          canComplete: completeStatuses.includes(data.status)
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  openCompleteForm() {
    wx.navigateTo({ url: `/pages/counselor/complete-form/index?id=${this.data.id}` });
  }
});
