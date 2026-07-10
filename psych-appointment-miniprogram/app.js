const auth = require('./utils/auth');

App({
  globalData: {
    student: null,
    counselor: null
  },

  onLaunch() {
    this.globalData.student = auth.getStudent();
    this.globalData.counselor = auth.getCounselor();
  }
});
