const config = require('../config/index');
const auth = require('./auth');

let authRedirecting = false;

function handleInvalidSession() {
  if (authRedirecting) {
    return;
  }

  authRedirecting = true;
  const redirectUrl = auth.getRole() === 'COUNSELOR'
    ? '/pages/counselor/login/index'
    : '/pages/login/login';

  auth.clearSession();
  wx.showToast({ title: '登录已失效，请重新登录', icon: 'none' });
  wx.reLaunch({
    url: redirectUrl,
    complete() {
      setTimeout(() => {
        authRedirecting = false;
      }, 1000);
    }
  });
}

function handlePasswordChangeRequired() {
  if (authRedirecting) {
    return;
  }
  authRedirecting = true;
  wx.reLaunch({
    url: '/pages/password-change/index',
    complete() {
      setTimeout(() => {
        authRedirecting = false;
      }, 1000);
    }
  });
}

function request(options) {
  const token = auth.getToken();
  const header = Object.assign(
    {
      'Content-Type': 'application/json'
    },
    options.header || {}
  );

  if (options.auth !== false && token) {
    header.Authorization = `Bearer ${token}`;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.baseUrl}${options.url}`,
      method: options.method || 'GET',
      data: options.data || {},
      header,
      timeout: options.timeout || 15000,
      success(res) {
        const body = res.data || {};
        const invalidSession = options.auth !== false
          && Boolean(token)
          && (res.statusCode === 401 || res.statusCode === 403);

        if (invalidSession) {
          if (res.statusCode === 403 && auth.requiresPasswordChange()) {
            handlePasswordChangeRequired();
            reject(new Error('Password change required'));
            return;
          }
          handleInvalidSession();
          reject(new Error('Session expired'));
          return;
        }
        if (res.statusCode < 200 || res.statusCode >= 300 || body.success === false) {
          const message = body.message || `请求失败 ${res.statusCode}`;
          wx.showToast({ title: message, icon: 'none' });
          reject(new Error(message));
          return;
        }
        resolve(body.data);
      },
      fail(error) {
        wx.showToast({ title: '网络连接失败', icon: 'none' });
        reject(error);
      }
    });
  });
}

module.exports = request;
