const config = require('../../../config/index');

function schoolContact(source, fallbackTitle, fallbackDesc) {
  const contact = source || {};
  const phone = String(contact.phone || '').trim();
  return {
    title: contact.title || fallbackTitle,
    phone,
    phoneText: phone,
    desc: contact.desc || fallbackDesc,
    available: Boolean(phone),
    actionText: '拨打'
  };
}

const schoolContacts = [
  schoolContact(
    config.emergencyContacts && config.emergencyContacts.schoolCounseling,
    '校内心理中心',
    '工作时间和服务地点请以学校正式通知为准'
  ),
  schoolContact(
    config.emergencyContacts && config.emergencyContacts.campusDuty,
    '校内值班电话',
    '夜间、周末或假期的校内值班渠道'
  ),
  schoolContact(
    config.emergencyContacts && config.emergencyContacts.schoolHospital,
    '校医院',
    '需要校内医疗支持时联系'
  )
].filter((item) => item.available);

Page({
  data: {
    immediateContacts: [
      {
        title: '医疗急救',
        phone: '120',
        desc: '出现身体伤害、服药过量或其他急危重情况',
        tone: 'medical'
      },
      {
        title: '报警求助',
        phone: '110',
        desc: '你或他人正面临立即的人身安全威胁',
        tone: 'police'
      }
    ],
    psychologicalHotline: {
      title: '全国心理援助热线',
      phone: '12356',
      desc: '提供心理健康教育、心理支持、心理疏导和危机干预服务'
    },
    schoolContacts,
    hasSchoolContacts: schoolContacts.length > 0,
    safetySteps: [
      '先离开可能造成伤害的环境，远离危险物品。',
      '联系一位可信任的同学、老师、家人或值班人员。',
      '在援助人员到达或电话接通前，尽量不要独处。'
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
