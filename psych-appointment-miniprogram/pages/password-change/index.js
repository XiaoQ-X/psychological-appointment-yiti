const request = require('../../utils/request');
const auth = require('../../utils/auth');

Page({
  data: {
    role: '',
    roleName: '',
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
    currentVisible: false,
    newVisible: false,
    confirmVisible: false,
    canSubmit: false,
    loading: false
  },

  onLoad() {
    const role = auth.getRole();
    if (!auth.getToken() || (role !== 'STUDENT' && role !== 'COUNSELOR')) {
      this.goToLogin(role);
      return;
    }
    this.setData({
      role,
      roleName: role === 'COUNSELOR' ? '咨询师' : '学生'
    });
  },

  onCurrentInput(event) {
    this.setData({ currentPassword: event.detail.value }, () => this.updateSubmitState());
  },

  onNewInput(event) {
    this.setData({ newPassword: event.detail.value }, () => this.updateSubmitState());
  },

  onConfirmInput(event) {
    this.setData({ confirmPassword: event.detail.value }, () => this.updateSubmitState());
  },

  toggleCurrentVisibility() {
    this.setData({ currentVisible: !this.data.currentVisible });
  },

  toggleNewVisibility() {
    this.setData({ newVisible: !this.data.newVisible });
  },

  toggleConfirmVisibility() {
    this.setData({ confirmVisible: !this.data.confirmVisible });
  },

  updateSubmitState() {
    const { currentPassword, newPassword, confirmPassword } = this.data;
    this.setData({
      canSubmit: Boolean(
        currentPassword
        && newPassword.length >= 8
        && confirmPassword
        && newPassword === confirmPassword
        && newPassword !== currentPassword
      )
    });
  },

  submit() {
    if (this.data.loading || !this.data.canSubmit) {
      return;
    }
    const endpoint = this.data.role === 'COUNSELOR'
      ? '/api/counselor/auth/change-password'
      : '/api/student/auth/change-password';
    this.setData({ loading: true });
    request({
      url: endpoint,
      method: 'POST',
      data: {
        currentPassword: this.data.currentPassword,
        newPassword: this.data.newPassword
      }
    })
      .then((data) => {
        const profile = auth.completePasswordChange(data);
        if (this.data.role === 'COUNSELOR') {
          getApp().globalData.counselor = profile;
        } else {
          getApp().globalData.student = profile;
        }
        wx.showToast({ title: '密码已更新', icon: 'success' });
        setTimeout(() => this.enterWorkspace(), 500);
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  switchAccount() {
    const role = this.data.role;
    auth.clearSession();
    getApp().globalData.student = null;
    getApp().globalData.counselor = null;
    this.goToLogin(role);
  },

  enterWorkspace() {
    if (this.data.role === 'COUNSELOR') {
      wx.reLaunch({ url: '/pages/counselor/appointments/index' });
      return;
    }
    wx.switchTab({ url: '/pages/student/counselors/index' });
  },

  goToLogin(role) {
    const target = role === 'COUNSELOR'
      ? '/pages/counselor/login/index'
      : '/pages/login/login';
    wx.reLaunch({ url: target });
  }
});
