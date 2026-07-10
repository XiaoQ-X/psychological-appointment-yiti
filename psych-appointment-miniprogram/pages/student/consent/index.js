Page({
  data: {
    sections: [
      {
        title: '服务对象',
        body: '本系统仅面向本校学生提供心理咨询预约服务。预约账号由学校统一导入和管理。'
      },
      {
        title: '保密原则',
        body: '心理咨询相关信息仅用于预约安排、咨询服务和必要的风险处理。未经授权，不会向无关人员公开。'
      },
      {
        title: '保密例外',
        body: '当出现可能危及你本人或他人安全的风险，或法律法规、学校危机干预流程要求时，心理中心可能联系相关人员提供必要支持。'
      },
      {
        title: '咨询边界',
        body: '心理咨询不等同于医疗诊断、急救或即时危机处置。如情况紧急，请优先联系线下值班人员、校医院或紧急热线。'
      },
      {
        title: '预约与取消',
        body: '请按预约时间到访。若需取消，请在系统允许的时间内提前处理；多次爽约可能影响后续预约。'
      },
      {
        title: '数据保护',
        body: '系统会对敏感字段进行权限控制和加密存储。后台查看敏感信息应记录审计日志。'
      }
    ]
  },

  agreeAndBack() {
    wx.navigateBack({
      fail: () => wx.switchTab({ url: '/pages/student/counselors/index' })
    });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  }
});
