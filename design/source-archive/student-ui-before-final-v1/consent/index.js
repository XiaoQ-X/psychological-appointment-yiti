const request = require('../../../utils/request');
const format = require('../../../utils/format');

Page({
  data: {
    fromBooking: false,
    hasReachedBottom: false,
    actionDisabled: false,
    actionText: '我已阅读',
    loadingVersion: true,
    versionLoadError: false,
    versionInfo: {
      id: null,
      versionNo: '本地说明',
      title: '心理咨询知情同意',
      content: '',
      publishedAtText: '版本信息加载中'
    },
    keyPoints: [
      {
        label: '保密原则',
        title: '你的信息会被谨慎保护',
        body: '预约和咨询信息仅用于服务安排、专业支持及必要的安全处理。'
      },
      {
        label: '保密例外',
        title: '安全风险下可能启动支持',
        body: '当你或他人的安全可能受到威胁时，心理中心可能联系必要人员。'
      },
      {
        label: '服务边界',
        title: '咨询不能替代医疗急救',
        body: '紧急危险、医疗急救或即时危机处置，请优先使用紧急渠道。'
      }
    ],
    sections: [
      {
        number: '01',
        title: '服务对象',
        body: '本系统仅面向本校学生提供心理咨询预约服务。预约账号由学校统一导入和管理。'
      },
      {
        number: '02',
        title: '保密原则',
        body: '心理咨询相关信息仅用于预约安排、咨询服务和必要的风险处理。未经授权，不会向无关人员公开。'
      },
      {
        number: '03',
        title: '保密例外',
        body: '当出现可能危及你本人或他人安全的风险，或法律法规、学校危机干预流程要求时，心理中心可能联系相关人员提供必要支持。'
      },
      {
        number: '04',
        title: '咨询边界',
        body: '心理咨询不等同于医疗诊断、急救或即时危机处置。如情况紧急，请优先联系线下值班人员、校医院或紧急热线。'
      },
      {
        number: '05',
        title: '预约与取消',
        body: '请按预约时间到访。若需取消，请在系统允许的时间内提前处理；多次爽约可能影响后续预约。'
      },
      {
        number: '06',
        title: '数据保护',
        body: '系统会对敏感字段进行权限控制和加密存储。后台查看敏感信息应记录审计日志。'
      }
    ]
  },

  onLoad(options) {
    const fromBooking = options.from === 'booking';
    this.setData({
      fromBooking,
      actionDisabled: fromBooking,
      actionText: fromBooking ? '请阅读至底部' : '我已阅读'
    });
    this.loadVersion();
  },

  onReachBottom() {
    if (!this.data.fromBooking || this.data.hasReachedBottom) {
      return;
    }
    this.setData({
      hasReachedBottom: true
    });
    this.refreshActionState();
  },

  loadVersion() {
    this.setData({ loadingVersion: true, versionLoadError: false });
    return request({
      url: '/api/public/consent/current',
      auth: false
    })
      .then((data) => {
        this.setData({
          versionInfo: {
            id: data.id,
            versionNo: data.versionNo || '当前版本',
            title: data.title || '心理咨询知情同意',
            content: data.content || '',
            publishedAtText: data.publishedAt
              ? `发布于 ${format.formatDateTime(data.publishedAt).slice(0, 10)}`
              : '最近更新日期暂缺'
          }
        });
        this.refreshActionState();
      })
      .catch(() => {
        this.setData({ versionLoadError: true });
        this.refreshActionState();
      })
      .finally(() => {
        this.setData({ loadingVersion: false });
      });
  },

  retryVersion() {
    this.loadVersion();
  },

  refreshActionState() {
    if (!this.data.fromBooking) {
      return;
    }
    const versionReady = Boolean(this.data.versionInfo.id);
    const actionDisabled = !this.data.hasReachedBottom || !versionReady;
    let actionText = '同意并返回预约';
    if (!this.data.hasReachedBottom) {
      actionText = '请阅读至底部';
    } else if (!versionReady) {
      actionText = '请先同步当前版本';
    }
    this.setData({ actionDisabled, actionText });
  },

  agreeAndBack() {
    if (this.data.fromBooking && !this.data.hasReachedBottom) {
      wx.showToast({ title: '请先阅读至页面底部', icon: 'none' });
      return;
    }
    if (this.data.fromBooking && !this.data.versionInfo.id) {
      wx.showToast({ title: '请先同步当前版本', icon: 'none' });
      return;
    }
    if (this.data.fromBooking && this.getOpenerEventChannel) {
      const eventChannel = this.getOpenerEventChannel();
      if (eventChannel && eventChannel.emit) {
        eventChannel.emit('consentAgreed', {
          id: this.data.versionInfo.id,
          versionNo: this.data.versionInfo.versionNo
        });
      }
    }
    wx.navigateBack({
      fail: () => wx.switchTab({ url: '/pages/student/counselors/index' })
    });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  }
});
