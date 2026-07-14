const request = require('../../utils/request');
const auth = require('../../utils/auth');

Page({
  data: {
    username: '',
    password: '',
    passwordVisible: false,
    passwordMode: true,
    canSubmit: false,
    loading: false
  },

  onLoad() {
    if (auth.getToken() && auth.getRole() === 'STUDENT') {
      wx.switchTab({ url: '/pages/student/counselors/index' });
      return;
    }
    if (auth.getToken() && auth.getRole() === 'COUNSELOR') {
      wx.redirectTo({ url: '/pages/counselor/appointments/index' });
    }
  },

  onUsernameInput(event) {
    const username = event.detail.value.trim();
    this.setData({
      username,
      canSubmit: Boolean(username && this.data.password)
    });
  },

  onPasswordInput(event) {
    const password = event.detail.value;
    this.setData({
      password,
      canSubmit: Boolean(this.data.username && password)
    });
  },

  clearUsername() {
    this.setData({ username: '', canSubmit: false });
  },

  togglePasswordVisibility() {
    const passwordVisible = !this.data.passwordVisible;
    this.setData({
      passwordVisible,
      passwordMode: !passwordVisible
    });
  },

  onPasswordConfirm() {
    this.submit();
  },

  submit() {
    if (this.data.loading) {
      return;
    }
    if (!this.data.username || !this.data.password) {
      wx.showToast({ title: '请输入学号和密码', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    request({
      url: '/api/student/auth/login',
      method: 'POST',
      auth: false,
      data: {
        username: this.data.username,
        password: this.data.password
      }
    })
      .then((data) => {
        auth.setSession(data);
        getApp().globalData.student = auth.getStudent();
        wx.switchTab({ url: '/pages/student/counselors/index' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  },

  openConsent() {
    wx.navigateTo({ url: '/pages/student/consent/index' });
  },

  openCounselorLogin() {
    wx.navigateTo({ url: '/pages/counselor/login/index' });
  }
});
