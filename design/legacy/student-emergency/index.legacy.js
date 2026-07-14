Page({
  data: {
    contacts: [
      { title: '校内心理中心', phone: '000-0000-1001', desc: '工作日 09:00-17:00，心理中心值班电话' },
      { title: '校内值班电话', phone: '000-0000-1002', desc: '夜间、周末或假期可联系值班人员' },
      { title: '校医院/合作医院', phone: '000-0000-1200', desc: '需要医疗支持时优先联系' },
      { title: '校外危机热线', phone: '000-0000-9999', desc: '无法联系校内人员时可作为补充渠道' }
    ]
  },

  callPhone(event) {
    const phone = event.currentTarget.dataset.phone;
    if (!phone) {
      return;
    }
    wx.makePhoneCall({ phoneNumber: phone });
  }
});
