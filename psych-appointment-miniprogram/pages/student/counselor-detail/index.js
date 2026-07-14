const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');
const config = require('../../../config/index');

const WEEKDAY_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];

function parseDateTime(value) {
  const date = new Date(`${value || ''}`.replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? null : date;
}

function slotDateLabel(value) {
  const date = parseDateTime(value);
  if (!date) {
    return '日期待定';
  }
  return `${date.getMonth() + 1}月${date.getDate()}日 ${WEEKDAY_LABELS[date.getDay()]}`;
}

function buildWeekDates(startDate, slots, selectedDateKey) {
  const counts = {};
  slots.forEach((slot) => {
    counts[slot.dateKey] = (counts[slot.dateKey] || 0) + 1;
  });
  return Array.from({ length: 7 }, (_, index) => {
    const date = format.addDays(startDate, index);
    const key = format.toDateString(date);
    const count = counts[key] || 0;
    return {
      key,
      weekday: WEEKDAY_LABELS[date.getDay()],
      dateText: `${date.getMonth() + 1}/${date.getDate()}`,
      metaText: index === 0 ? '今天' : (count ? `${count}个` : '暂无'),
      activeClass: key === selectedDateKey ? 'active' : '',
      disabledClass: count ? '' : 'disabled'
    };
  });
}

function groupSlots(slots) {
  return slots.reduce((groups, slot) => {
    const existing = groups.find((item) => item.dateKey === slot.dateKey);
    if (existing) {
      existing.slots.push(slot);
    } else {
      groups.push({ dateKey: slot.dateKey, dateText: slot.dateText, slots: [slot] });
    }
    return groups;
  }, []);
}

Page({
  data: {
    id: null,
    initialSlotId: null,
    loading: true,
    counselor: {},
    slots: [],
    slotGroups: [],
    visibleSlotGroups: [],
    visibleSlotCount: 0,
    selectedDateKey: '',
    selectedSlotId: null,
    weekDates: [],
    introExpanded: false
  },

  onLoad(options) {
    this.setData({
      id: options.id,
      initialSlotId: options.slotId ? Number(options.slotId) : null
    });
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
    this.setData({ loading: true });
    const today = new Date();
    const from = format.toDateString(today);
    const to = format.toDateString(format.addDays(today, config.defaultLookAheadDays));
    return Promise.all([
      request({ url: `/api/student/counselors/${this.data.id}?from=${from}&to=${to}` }),
      request({ url: `/api/student/counselors/${this.data.id}/slots?from=${from}&to=${to}` })
    ])
      .then(([counselor, slots]) => {
        const normalizedCounselor = {
          ...counselor,
          expertise: (counselor.expertise || []).slice(0, 3),
          avatarUrl: counselor.avatarUrl || '',
          initial: `${counselor.name || '咨询'}`.slice(0, 1),
          titleText: counselor.title || '心理咨询师',
          campusText: counselor.campusName || '校内咨询',
          hasExpertise: Boolean(counselor.expertise && counselor.expertise.length),
          introText: counselor.intro || '暂无简介'
        };
        const initialSlotId = this.data.initialSlotId;
        const normalizedSlots = (slots || []).map((slot) => {
          const fullStart = format.formatDateTime(slot.startAt);
          return {
            ...slot,
            dateKey: fullStart.slice(0, 10),
            dateText: slotDateLabel(slot.startAt),
            timeOnlyText: `${fullStart.slice(11)}-${format.formatDateTime(slot.endAt).slice(11)}`,
            timeText: format.formatTimeRange(slot.startAt, slot.endAt),
            placeText: slot.roomName || slot.campusName || '校内咨询',
            modeText: slot.serviceTypeName || '个体线下咨询',
            selectedClass: Number(slot.id) === initialSlotId ? 'selected' : ''
          };
        });
        const initialSlot = normalizedSlots.find((slot) => Number(slot.id) === initialSlotId);
        const currentDateKey = this.data.selectedDateKey;
        const hasCurrentDate = normalizedSlots.some((slot) => slot.dateKey === currentDateKey);
        const selectedDateKey = initialSlot
          ? initialSlot.dateKey
          : (hasCurrentDate ? currentDateKey : (normalizedSlots[0] ? normalizedSlots[0].dateKey : ''));
        const slotGroups = groupSlots(normalizedSlots);
        const visibleSlotGroups = selectedDateKey
          ? slotGroups.filter((group) => group.dateKey === selectedDateKey)
          : slotGroups;
        this.setData({
          counselor: normalizedCounselor,
          slots: normalizedSlots,
          slotGroups,
          visibleSlotGroups,
          visibleSlotCount: visibleSlotGroups.reduce((total, group) => total + group.slots.length, 0),
          selectedDateKey,
          selectedSlotId: initialSlot ? initialSlot.id : null,
          weekDates: buildWeekDates(today, normalizedSlots, selectedDateKey)
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  selectDate(event) {
    const selectedDateKey = event.currentTarget.dataset.key;
    const visibleSlotGroups = this.data.slotGroups.filter((group) => group.dateKey === selectedDateKey);
    this.setData({
      selectedDateKey,
      visibleSlotGroups,
      visibleSlotCount: visibleSlotGroups.reduce((total, group) => total + group.slots.length, 0),
      weekDates: this.data.weekDates.map((item) => ({
        ...item,
        activeClass: item.key === selectedDateKey ? 'active' : ''
      }))
    });
  },

  showAllSlots() {
    this.setData({
      selectedDateKey: '',
      visibleSlotGroups: this.data.slotGroups,
      visibleSlotCount: this.data.slots.length,
      weekDates: this.data.weekDates.map((item) => ({ ...item, activeClass: '' }))
    });
  },

  selectSlot(event) {
    const selectedSlotId = Number(event.currentTarget.dataset.id);
    const selectedSlot = this.data.slots.find((slot) => Number(slot.id) === selectedSlotId);
    const slots = this.data.slots.map((slot) => ({
      ...slot,
      selectedClass: Number(slot.id) === selectedSlotId ? 'selected' : ''
    }));
    const slotGroups = groupSlots(slots);
    const selectedDateKey = selectedSlot ? selectedSlot.dateKey : this.data.selectedDateKey;
    const visibleSlotGroups = selectedDateKey
      ? slotGroups.filter((group) => group.dateKey === selectedDateKey)
      : slotGroups;
    this.setData({
      selectedSlotId,
      selectedDateKey,
      slots,
      slotGroups,
      visibleSlotGroups,
      visibleSlotCount: visibleSlotGroups.reduce((total, group) => total + group.slots.length, 0),
      weekDates: this.data.weekDates.map((item) => ({
        ...item,
        activeClass: item.key === selectedDateKey ? 'active' : ''
      }))
    });
  },

  toggleIntro() {
    this.setData({ introExpanded: !this.data.introExpanded });
  },

  goBooking() {
    const slot = this.data.slots.find((item) => Number(item.id) === Number(this.data.selectedSlotId));
    if (!slot) {
      return;
    }
    wx.navigateTo({
      url: `/pages/student/booking-form/index?slotId=${slot.id}&counselorId=${this.data.id}&time=${encodeURIComponent(slot.timeText)}&name=${encodeURIComponent(this.data.counselor.name || '')}&place=${encodeURIComponent(slot.placeText || '')}`
    });
  }
});
