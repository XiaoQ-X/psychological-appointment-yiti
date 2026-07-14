const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');
const config = require('../../../config/index');

const WEEKDAY_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

function slotDateLabel(value) {
  const date = new Date(`${value || ''}`.replace(' ', 'T'));
  if (Number.isNaN(date.getTime())) {
    return '日期待定';
  }
  return `${date.getMonth() + 1}月${date.getDate()}日 ${WEEKDAY_LABELS[date.getDay()]}`;
}

Page({
  data: {
    id: null,
    loading: true,
    counselor: {},
    slots: [],
    slotGroups: [],
    selectedSlotId: null
  },

  onLoad(options) {
    this.setData({ id: options.id });
  },

  onShow() {
    if (!auth.ensureLogin()) {
      return;
    }
    this.loadData();
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh());
  },

  loadData() {
    this.setData({ loading: true, selectedSlotId: null });
    const from = format.toDateString(new Date());
    const to = format.toDateString(format.addDays(new Date(), config.defaultLookAheadDays));
    return Promise.all([
      request({ url: `/api/student/counselors/${this.data.id}?from=${from}&to=${to}` }),
      request({ url: `/api/student/counselors/${this.data.id}/slots?from=${from}&to=${to}` })
    ])
      .then(([counselor, slots]) => {
        const normalizedCounselor = {
          ...counselor,
          expertise: counselor.expertise || [],
          avatarUrl: counselor.avatarUrl || '',
          titleText: counselor.title || '心理咨询师',
          campusText: counselor.campusName || '校内咨询',
          slotCount: counselor.availableSlotCount || 0,
          hasExpertise: Boolean(counselor.expertise && counselor.expertise.length),
          introText: counselor.intro || '暂无简介'
        };
        const normalizedSlots = (slots || []).map((slot) => {
          const fullStart = format.formatDateTime(slot.startAt);
          return {
            ...slot,
            dateKey: fullStart.slice(0, 10),
            dateText: slotDateLabel(slot.startAt),
            timeOnlyText: `${fullStart.slice(11)}-${format.formatDateTime(slot.endAt).slice(11)}`,
            timeText: format.formatTimeRange(slot.startAt, slot.endAt),
            placeText: slot.roomName || slot.campusName || '校内咨询',
            modeText: slot.serviceTypeName || '线下咨询',
            selectedClass: ''
          };
        });
        const grouped = normalizedSlots.reduce((groups, slot) => {
          const existing = groups.find((item) => item.dateKey === slot.dateKey);
          if (existing) {
            existing.slots.push(slot);
          } else {
            groups.push({ dateKey: slot.dateKey, dateText: slot.dateText, slots: [slot] });
          }
          return groups;
        }, []);
        this.setData({
          counselor: normalizedCounselor,
          slots: normalizedSlots,
          slotGroups: grouped
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  selectSlot(event) {
    const selectedSlotId = Number(event.currentTarget.dataset.id);
    const slots = this.data.slots.map((slot) => ({
      ...slot,
      selectedClass: slot.id === selectedSlotId ? 'selected' : ''
    }));
    const slotGroups = this.data.slotGroups.map((group) => ({
      ...group,
      slots: group.slots.map((slot) => ({
        ...slot,
        selectedClass: slot.id === selectedSlotId ? 'selected' : ''
      }))
    }));
    this.setData({ selectedSlotId, slots, slotGroups });
  },

  goBooking() {
    const slot = this.data.slots.find((item) => item.id === this.data.selectedSlotId);
    if (!slot) {
      return;
    }
    wx.navigateTo({
      url: `/pages/student/booking-form/index?slotId=${slot.id}&counselorId=${this.data.id}&time=${encodeURIComponent(slot.timeText)}&name=${encodeURIComponent(this.data.counselor.name || '')}`
    });
  }
});
