const request = require('../../../utils/request');
const auth = require('../../../utils/auth');
const format = require('../../../utils/format');

const WEEKDAY_LABELS = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
const ACTIVE_APPOINTMENT_STATUSES = ['CONFIRMED', 'RISK_REVIEW', 'COUNSELOR_REVIEW', 'ADMIN_REVIEW'];

function parseDateTime(value) {
  const date = new Date(`${value || ''}`.replace(' ', 'T'));
  return Number.isNaN(date.getTime()) ? null : date;
}

function toDateKey(value) {
  const date = value instanceof Date ? value : parseDateTime(value);
  if (!date) {
    return '';
  }
  return format.toDateString(date);
}

function slotTimeText(slot) {
  const start = parseDateTime(slot.startAt);
  const end = parseDateTime(slot.endAt);
  if (!start || !end) {
    return '时间待定';
  }
  const pad = (number) => `${number}`.padStart(2, '0');
  return `${WEEKDAY_LABELS[start.getDay()]} ${pad(start.getHours())}:${pad(start.getMinutes())}-${pad(end.getHours())}:${pad(end.getMinutes())}`;
}

function normalizeSlots(slots) {
  return (slots || []).map((slot) => ({
    ...slot,
    dateKey: toDateKey(slot.startAt),
    shortText: slotTimeText(slot)
  }));
}

function buildWeekDates(startDate, counselors, selectedDateKey) {
  const counts = {};
  counselors.forEach((counselor) => {
    counselor.weekSlots.forEach((slot) => {
      counts[slot.dateKey] = (counts[slot.dateKey] || 0) + 1;
    });
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
      count,
      activeClass: key === selectedDateKey ? 'active' : '',
      disabledClass: count ? '' : 'disabled'
    };
  });
}

function filterCounselors(allCounselors, selectedDateKey, activeFilter) {
  const stressKeywords = ['压力', '情绪', '学业', '适应', '焦虑'];
  return allCounselors
    .filter((item) => {
      if (activeFilter === 'STRESS') {
        return item.expertise.some((label) => stressKeywords.some((keyword) => `${label}`.includes(keyword)));
      }
      if (activeFilter === 'WEEK') {
        return item.weekSlots.length > 0;
      }
      return true;
    })
    .map((item) => {
      const matchingSlots = selectedDateKey
        ? item.weekSlots.filter((slot) => slot.dateKey === selectedDateKey)
        : item.weekSlots;
      return {
        ...item,
        visibleSlots: matchingSlots.slice(0, 2)
      };
    })
    .filter((item) => !selectedDateKey || item.visibleSlots.length > 0)
    .sort((left, right) => {
      const leftTime = left.visibleSlots[0] ? `${left.visibleSlots[0].startAt}` : '9999';
      const rightTime = right.visibleSlots[0] ? `${right.visibleSlots[0].startAt}` : '9999';
      return leftTime.localeCompare(rightTime);
    });
}

Page({
  data: {
    loading: true,
    allCounselors: [],
    counselors: [],
    activeFilter: 'ALL',
    selectedDateKey: '',
    weekDates: [],
    nextAppointment: null,
    banners: [
      {
        title: '考试周压力支持',
        description: '本周仍有可约时段，给自己留一点喘息。',
        linkText: '查看可约咨询师',
        image: '/assets/design/carousel-exam.png',
        action: 'counselors'
      },
      {
        title: '首次咨询准备',
        description: '提前了解流程，让第一次见面更从容。',
        linkText: '查看服务须知',
        image: '/assets/design/carousel-prepare.png',
        action: 'consent'
      },
      {
        title: '预约与取消规则',
        description: '合理安排时间，需要调整时请提前处理。',
        linkText: '了解预约规则',
        image: '/assets/design/carousel-rules.png',
        action: 'consent'
      }
    ]
  },

  onShow() {
    if (!auth.ensureLogin()) {
      return;
    }
    this.loadCounselors();
  },

  onPullDownRefresh() {
    this.loadCounselors().finally(() => wx.stopPullDownRefresh());
  },

  loadCounselors() {
    this.setData({ loading: true });
    const today = new Date();
    const from = format.toDateString(today);
    const to = format.toDateString(format.addDays(today, 6));

    return Promise.all([
      request({ url: `/api/student/counselors?from=${from}&to=${to}` }),
      request({ url: '/api/student/appointments' }).catch(() => [])
    ])
      .then(([data, appointments]) => {
        const rawCounselors = data || [];
        return Promise.all(rawCounselors.map((item) => (
          request({ url: `/api/student/counselors/${item.id}/slots?from=${from}&to=${to}` })
            .catch(() => [])
        ))).then((slotLists) => ({ rawCounselors, slotLists, appointments }));
      })
      .then(({ rawCounselors, slotLists, appointments }) => {
        const allCounselors = rawCounselors.map((item, index) => {
          const weekSlots = normalizeSlots(slotLists[index]);
          return {
            ...item,
            expertise: (item.expertise || []).slice(0, 3),
            avatarUrl: item.avatarUrl || '',
            initial: `${item.name || '咨询'}`.slice(0, 1),
            titleText: item.title || '心理咨询师',
            campusText: item.campusName || '校内咨询',
            weekSlots,
            weekAvailabilityText: weekSlots.length ? `本周可约 ${weekSlots.length} 个时段` : '本周暂无时段',
            visibleSlots: []
          };
        });

        const dateCounts = {};
        allCounselors.forEach((item) => {
          item.weekSlots.forEach((slot) => {
            dateCounts[slot.dateKey] = (dateCounts[slot.dateKey] || 0) + 1;
          });
        });
        const currentSelection = this.data.selectedDateKey;
        const selectedDateKey = dateCounts[currentSelection]
          ? currentSelection
          : (Object.keys(dateCounts).sort()[0] || '');
        const counselors = filterCounselors(allCounselors, selectedDateKey, this.data.activeFilter);
        const now = Date.now();
        const next = appointments
          .filter((item) => {
            const endAt = parseDateTime(item.endAt);
            return ACTIVE_APPOINTMENT_STATUSES.includes(item.status)
              && endAt
              && endAt.getTime() >= now;
          })
          .sort((left, right) => `${left.startAt}`.localeCompare(`${right.startAt}`))[0];

        this.setData({
          allCounselors,
          counselors,
          selectedDateKey,
          weekDates: buildWeekDates(today, allCounselors, selectedDateKey),
          nextAppointment: next ? {
            ...next,
            timeText: format.formatTimeRange(next.startAt, next.endAt),
            statusText: format.statusText(next.status),
            tagType: format.statusTagType(next.status),
            counselorText: next.counselorName || '咨询师',
            placeText: `${next.campusName || '校内咨询'} ${next.roomName || ''}`.trim(),
            id: next.appointmentId
          } : null
        });
      })
      .finally(() => {
        this.setData({ loading: false });
      });
  },

  selectDate(event) {
    const selectedDateKey = event.currentTarget.dataset.key;
    const counselors = filterCounselors(this.data.allCounselors, selectedDateKey, this.data.activeFilter);
    this.setData({
      selectedDateKey,
      counselors,
      weekDates: this.data.weekDates.map((item) => ({
        ...item,
        activeClass: item.key === selectedDateKey ? 'active' : ''
      }))
    });
  },

  showAllDates() {
    this.setData({
      selectedDateKey: '',
      counselors: filterCounselors(this.data.allCounselors, '', this.data.activeFilter),
      weekDates: this.data.weekDates.map((item) => ({ ...item, activeClass: '' }))
    });
  },

  handleBannerTap(event) {
    const action = event.currentTarget.dataset.action;
    if (action === 'consent') {
      this.openConsent();
      return;
    }
    wx.pageScrollTo({ selector: '#counselor-list', duration: 260 });
  },

  openPreviewSlot(event) {
    const counselorId = event.currentTarget.dataset.counselorId;
    const slotId = event.currentTarget.dataset.slotId;
    wx.navigateTo({
      url: `/pages/student/counselor-detail/index?id=${counselorId}&slotId=${slotId}`
    });
  },

  openDetail(event) {
    const id = event.currentTarget.dataset.id;
    wx.navigateTo({ url: `/pages/student/counselor-detail/index?id=${id}` });
  },

  openNextAppointment() {
    if (this.data.nextAppointment && this.data.nextAppointment.id) {
      wx.navigateTo({ url: `/pages/student/appointment-detail/index?id=${this.data.nextAppointment.id}` });
      return;
    }
    wx.pageScrollTo({ selector: '#counselor-list', duration: 260 });
  },

  openEmergency() {
    wx.navigateTo({ url: '/pages/student/emergency/index' });
  },

  openConsent() {
    wx.navigateTo({ url: '/pages/student/consent/index' });
  },

  logout() {
    auth.clearSession();
    wx.reLaunch({ url: '/pages/login/login' });
  }
});
