const request = require('../../../utils/request');
const auth = require('../../../utils/auth');

const urgencyValues = ['LOW', 'MEDIUM', 'HIGH'];

Page({
  data: {
    slotId: null,
    counselorId: null,
    counselorName: '',
    slotTime: '',
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
    expectedHelp: '',
    urgencyLabels: ['一般', '较急', '紧急'],
    urgencyIndex: 0,
    contactTime: '',
    riskOptions: [
      { value: 'selfHarm', label: '近期有自伤或自杀想法' },
      { value: 'harmOthers', label: '近期有伤害他人的想法' },
      { value: 'crisisEvent', label: '近期经历重大危机事件' },
      { value: 'psychiatricTreatment', label: '正在或曾经接受精神科治疗' },
      { value: 'medication', label: '正在服用精神类药物' },
      { value: 'willingContact', label: '愿意在必要时接受联系', checked: true }
    ],
    riskValues: ['willingContact'],
    hasHighRisk: false,
    consentAgreed: false,
    submitting: false
  },

  onLoad(options) {
    this.setData({
      slotId: Number(options.slotId),
      counselorId: Number(options.counselorId),
      counselorName: decodeURIComponent(options.name || ''),
      slotTime: decodeURIComponent(options.time || '')
    });
  },

  onShow() {
    auth.ensureLogin();
  },

  onFirstVisitChange(event) {
    this.setData({ firstVisit: event.detail.value === 'true' });
  },

  onIssueChange(event) {
    this.setData({ issueTypes: event.detail.value });
  },

  onDescriptionInput(event) {
    this.setData({ description: event.detail.value });
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

  onRiskChange(event) {
    const values = event.detail.value;
    this.setData({
      riskValues: values,
      hasHighRisk: values.includes('selfHarm') || values.includes('harmOthers') || values.includes('crisisEvent')
    });
  },

  toggleConsent() {
    this.setData({ consentAgreed: !this.data.consentAgreed });
  },

  submit() {
    if (!this.data.issueTypes.length) {
      wx.showToast({ title: '请选择主要困扰', icon: 'none' });
      return;
    }
    if (!this.data.description.trim()) {
      wx.showToast({ title: '请填写情况说明', icon: 'none' });
      return;
    }
    if (!this.data.consentAgreed) {
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
    const riskValues = this.data.riskValues;
    return {
      slotId: this.data.slotId,
      firstVisit: this.data.firstVisit,
      issueTypes: this.data.issueTypes,
      description: this.data.description.trim(),
      expectedHelp: this.data.expectedHelp.trim(),
      urgencyLevel: urgencyValues[this.data.urgencyIndex],
      contactTime: this.data.contactTime.trim(),
      consentAgreed: this.data.consentAgreed,
      risk: {
        selfHarm: riskValues.includes('selfHarm'),
        harmOthers: riskValues.includes('harmOthers'),
        crisisEvent: riskValues.includes('crisisEvent'),
        psychiatricTreatment: riskValues.includes('psychiatricTreatment'),
        medication: riskValues.includes('medication'),
        willingContact: riskValues.includes('willingContact')
      }
    };
  },

  openConsent() {
    wx.navigateTo({ url: '/pages/student/consent/index' });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  }
});
