const request = require('../../../utils/request');
const auth = require('../../../utils/auth');

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  onLoad() {
    if (auth.getToken() && auth.requiresPasswordChange()) {
      auth.openPasswordChange();
      return;
    }
    if (auth.getToken() && auth.getRole() === 'COUNSELOR') {
      wx.redirectTo({ url: '/pages/counselor/appointments/index' });
      return;
    }
    if (auth.getToken() && auth.getRole() === 'STUDENT') {
      wx.switchTab({ url: '/pages/student/counselors/index' });
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
      wx.showToast({ title: '请输入账号和密码', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    request({
      url: '/api/counselor/auth/login',
      method: 'POST',
      auth: false,
      data: {
        username: this.data.username,
        password: this.data.password
      }
    })
      .then((data) => {
        auth.setCounselorSession(data);
        getApp().globalData.counselor = auth.getCounselor();
        if (data.forcePasswordChange) {
          wx.redirectTo({ url: '/pages/password-change/index' });
          return;
        }
        wx.redirectTo({ url: '/pages/counselor/appointments/index' });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  openStudentLogin() {
    wx.reLaunch({ url: '/pages/login/login' });
  }
});
