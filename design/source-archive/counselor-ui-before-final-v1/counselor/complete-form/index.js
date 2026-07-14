const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');

Page({
  data: {
    id: null,
    loading: true,
    submitting: false,
    submitDisabled: true,
    detail: {},
    topic: '',
    summary: '',
    followUpPlan: '',
    needReferral: false,
    riskChangeIndex: 0,
    selectedRiskLabel: '无明显变化',
    riskOptions: [
      { label: '无明显变化', value: 'STABLE' },
      { label: '风险降低', value: 'LOWER' },
      { label: '风险升高', value: 'HIGHER' },
      { label: '暂不判断', value: '' }
    ]
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

  loadDetail() {
    this.setData({ loading: true });
    return request({ url: `/api/counselor/appointments/${this.data.id}` })
      .then((data) => {
        this.setData({
          detail: {
            ...data,
            studentText: data.studentName || data.studentNo || '学生',
            timeText: format.formatTimeRange(data.startAt, data.endAt),
            placeText: `${data.campusName || '校内咨询'} ${data.roomName || ''}`,
            statusText: format.statusText(data.status),
            tagType: format.statusTagType(data.status)
          }
        });
      })
      .finally(() => {
        this.setData({ loading: false });
        this.updateSubmitDisabled();
      });
  },

  onTopicInput(event) {
    this.setData({ topic: event.detail.value });
    this.updateSubmitDisabled();
  },

  onSummaryInput(event) {
    this.setData({ summary: event.detail.value });
    this.updateSubmitDisabled();
  },

  onFollowUpPlanInput(event) {
    this.setData({ followUpPlan: event.detail.value });
  },

  onRiskChange(event) {
    const riskChangeIndex = Number(event.detail.value);
    this.setData({
      riskChangeIndex,
      selectedRiskLabel: this.data.riskOptions[riskChangeIndex].label
    });
  },

  onNeedReferralChange(event) {
    this.setData({ needReferral: event.detail.value });
  },

  updateSubmitDisabled() {
    const disabled = this.data.submitting || !this.data.topic.trim() || !this.data.summary.trim();
    this.setData({ submitDisabled: disabled });
  },

  submit() {
    const topic = this.data.topic.trim();
    const summary = this.data.summary.trim();
    if (!topic || !summary) {
      wx.showToast({ title: '请填写主题和记录摘要', icon: 'none' });
      return;
    }
    this.setData({ submitting: true, submitDisabled: true });
    request({
      url: `/api/counselor/appointments/${this.data.id}/complete`,
      method: 'POST',
      data: {
        topic,
        summary,
        riskChange: this.data.riskOptions[this.data.riskChangeIndex].value,
        followUpPlan: this.data.followUpPlan.trim(),
        needReferral: this.data.needReferral
      }
    })
      .then(() => {
        wx.showToast({ title: '已完成记录', icon: 'success' });
        wx.redirectTo({ url: `/pages/counselor/appointment-detail/index?id=${this.data.id}` });
      })
      .finally(() => {
        this.setData({ submitting: false });
        this.updateSubmitDisabled();
      });
  }
});
