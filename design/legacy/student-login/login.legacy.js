const request = require('../../utils/request');
const auth = require('../../utils/auth');

Page({
  data: {
    username: '',
    password: '',
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
    this.setData({ username: event.detail.value.trim() });
  },

  onPasswordInput(event) {
    this.setData({ password: event.detail.value });
  },

  submit() {
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
