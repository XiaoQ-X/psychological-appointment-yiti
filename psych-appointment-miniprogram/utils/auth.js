const TOKEN_KEY = 'student_access_token';
const STUDENT_KEY = 'student_profile';
const COUNSELOR_KEY = 'counselor_profile';

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || '';
}

function getRole() {
  const counselor = getCounselor();
  if (counselor && counselor.role) {
    return counselor.role;
  }
  const student = getStudent();
  return student && student.role ? student.role : '';
}

function setSession(loginData) {
  wx.setStorageSync(TOKEN_KEY, loginData.accessToken);
  wx.removeStorageSync(COUNSELOR_KEY);
  const student = {
    accountId: loginData.accountId,
    studentId: loginData.studentId,
    username: loginData.username,
    role: loginData.role,
    forcePasswordChange: loginData.forcePasswordChange
  };
  wx.setStorageSync(STUDENT_KEY, student);
  return student;
}

function setCounselorSession(loginData) {
  wx.setStorageSync(TOKEN_KEY, loginData.accessToken);
  wx.removeStorageSync(STUDENT_KEY);
  const counselor = {
    accountId: loginData.accountId,
    counselorId: loginData.counselorId,
    username: loginData.username,
    counselorName: loginData.counselorName,
    role: loginData.role,
    forcePasswordChange: loginData.forcePasswordChange
  };
  wx.setStorageSync(COUNSELOR_KEY, counselor);
  return counselor;
}

function getStudent() {
  return wx.getStorageSync(STUDENT_KEY) || null;
}

function getCounselor() {
  return wx.getStorageSync(COUNSELOR_KEY) || null;
}

function clearSession() {
  wx.removeStorageSync(TOKEN_KEY);
  wx.removeStorageSync(STUDENT_KEY);
  wx.removeStorageSync(COUNSELOR_KEY);
}

function ensureLogin() {
  if (!getToken() || getRole() !== 'STUDENT') {
    wx.reLaunch({ url: '/pages/login/login' });
    return false;
  }
  return true;
}

function ensureCounselorLogin() {
  if (!getToken() || getRole() !== 'COUNSELOR') {
    wx.reLaunch({ url: '/pages/counselor/login/index' });
    return false;
  }
  return true;
}

module.exports = {
  getToken,
  getRole,
  setSession,
  setCounselorSession,
  getStudent,
  getCounselor,
  clearSession,
  ensureLogin,
  ensureCounselorLogin
};
