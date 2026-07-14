const request = require('../../../utils/request');
const auth = require('../../../utils/auth');

const urgencyValues = ['LOW', 'MEDIUM', 'HIGH'];

Page({
  data: {
    slotId: null,
    counselorId: null,
    counselorName: '',
    slotTime: '',
    placeText: '',
    firstVisit: true,
    issueOptions: [
      { value: 'academic-stress', label: '学业压力' },
      { value: 'emotion', label: '情绪困扰' },
      { value: 'relationship', label: '人际关系' },
      { value: 'family', label: '家庭议题' },
      { value: 'sleep', label: '睡眠问题' },
      { value: 'career', label: '生涯发展' }
    ],
    issueTypes: [],
    description: '',
    descriptionLength: 0,
    expectedHelp: '',
    urgencyLabels: ['一般', '较急', '紧急'],
    urgencyIndex: 0,
    contactTime: '',
    riskQuestions: [
      { value: 'selfHarm', label: '在过去两周内，是否有过伤害自己或结束生命的想法？', answer: false },
      { value: 'harmOthers', label: '在过去两周内，是否有过伤害他人的想法？', answer: false },
      { value: 'crisisEvent', label: '近期是否经历过重大危机事件？', answer: false },
      { value: 'psychiatricTreatment', label: '是否正在或曾经接受精神科治疗？', answer: false },
      { value: 'medication', label: '是否正在服用精神类药物？', answer: false }
    ],
    hasHighRisk: false,
    consentVersionId: null,
    consentVersionNo: '',
    consentAgreed: false,
    canSubmit: false,
    completionHint: '还需完成：主要困扰、情况说明、知情同意',
    submitting: false
  },

  onLoad(options) {
    this.setData({
      slotId: Number(options.slotId),
      counselorId: Number(options.counselorId),
      counselorName: decodeURIComponent(options.name || ''),
      slotTime: decodeURIComponent(options.time || ''),
      placeText: decodeURIComponent(options.place || '')
    });
  },

  onShow() {
    auth.ensureLogin();
  },

  onFirstVisitChange(event) {
    this.setData({ firstVisit: event.detail.value === 'true' });
  },

  onIssueChange(event) {
    const issueTypes = event.detail.value;
    this.setData({
      issueTypes,
      issueOptions: this.data.issueOptions.map((item) => ({
        ...item,
        selectedClass: issueTypes.includes(item.value) ? 'selected' : ''
      }))
    }, () => this.refreshSubmitState());
  },

  onDescriptionInput(event) {
    const description = event.detail.value.slice(0, 300);
    this.setData({ description, descriptionLength: description.length }, () => this.refreshSubmitState());
  },

  onExpectedHelpInput(event) {
    this.setData({ expectedHelp: event.detail.value });
  },

  onUrgencyChange(event) {
    this.setData({ urgencyIndex: Number(event.detail.value) });
  },

  onContactTimeInput(event) {
    this.setData({ contactTime: event.detail.value });
  },

  onRiskAnswerChange(event) {
    const value = event.currentTarget.dataset.value;
    const answer = event.detail.value === 'true';
    const riskQuestions = this.data.riskQuestions.map((item) => (
      item.value === value ? { ...item, answer } : item
    ));
    this.setData({
      riskQuestions,
      hasHighRisk: riskQuestions.some((item) => (
        item.answer && ['selfHarm', 'harmOthers', 'crisisEvent'].includes(item.value)
      ))
    });
  },

  toggleConsent() {
    if (this.data.consentAgreed) {
      this.setData({
        consentAgreed: false,
        consentVersionId: null,
        consentVersionNo: ''
      }, () => this.refreshSubmitState());
      return;
    }
    this.openConsent();
  },

  submit() {
    if (!this.data.canSubmit) {
      wx.showToast({ title: this.data.completionHint, icon: 'none' });
      return;
    }
    if (!this.data.issueTypes.length) {
      wx.showToast({ title: '请选择主要困扰', icon: 'none' });
      return;
    }
    if (!this.data.description.trim()) {
      wx.showToast({ title: '请填写情况说明', icon: 'none' });
      return;
    }
    if (!this.data.consentAgreed || !this.data.consentVersionId) {
      wx.showToast({ title: '请确认知情同意', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    request({
      url: `/api/student/appointments/slots/${this.data.slotId}/lock`,
      method: 'POST'
    })
      .then(() => request({
        url: '/api/student/appointments',
        method: 'POST',
        data: this.buildPayload()
      }))
      .then((data) => {
        const status = data.status || (this.data.hasHighRisk ? 'RISK_REVIEW' : 'CONFIRMED');
        wx.redirectTo({
          url: `/pages/student/booking-result/index?id=${data.appointmentId}&status=${status}&name=${encodeURIComponent(this.data.counselorName)}&time=${encodeURIComponent(this.data.slotTime)}`
        });
      })
      .finally(() => {
        this.setData({ submitting: false });
      });
  },

  buildPayload() {
    const riskAnswers = this.data.riskQuestions.reduce((values, item) => {
      values[item.value] = item.answer;
      return values;
    }, {});
    return {
      slotId: this.data.slotId,
      firstVisit: this.data.firstVisit,
      issueTypes: this.data.issueTypes,
      description: this.data.description.trim(),
      expectedHelp: this.data.expectedHelp.trim(),
      urgencyLevel: urgencyValues[this.data.urgencyIndex],
      contactTime: this.data.contactTime.trim(),
      consentVersionId: this.data.consentVersionId,
      consentAgreed: this.data.consentAgreed,
      risk: {
        selfHarm: Boolean(riskAnswers.selfHarm),
        harmOthers: Boolean(riskAnswers.harmOthers),
        crisisEvent: Boolean(riskAnswers.crisisEvent),
        psychiatricTreatment: Boolean(riskAnswers.psychiatricTreatment),
        medication: Boolean(riskAnswers.medication),
        willingContact: true
      }
    };
  },

  refreshSubmitState() {
    const missing = [];
    if (!this.data.issueTypes.length) {
      missing.push('主要困扰');
    }
    if (!this.data.description.trim()) {
      missing.push('情况说明');
    }
    if (!this.data.consentAgreed || !this.data.consentVersionId) {
      missing.push('知情同意');
    }
    this.setData({
      canSubmit: missing.length === 0,
      completionHint: missing.length ? `还需完成：${missing.join('、')}` : '信息已完整，可以提交'
    });
  },

  openConsent() {
    wx.navigateTo({
      url: '/pages/student/consent/index?from=booking',
      events: {
        consentAgreed: (consent) => {
          this.setData({
            consentAgreed: true,
            consentVersionId: consent.id,
            consentVersionNo: consent.versionNo || ''
          }, () => this.refreshSubmitState());
        }
      }
    });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  }
});
