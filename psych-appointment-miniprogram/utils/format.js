function pad(value) {
  return value < 10 ? `0${value}` : `${value}`;
}

function toDateString(date) {
  const value = date instanceof Date ? date : new Date(date);
  return `${value.getFullYear()}-${pad(value.getMonth() + 1)}-${pad(value.getDate())}`;
}

function addDays(date, days) {
  const value = new Date(date);
  value.setDate(value.getDate() + days);
  return value;
}

function formatDateTime(value) {
  if (!value) {
    return '';
  }
  const date = parseDateTime(value);
  if (Number.isNaN(date.getTime())) {
    return '';
  }
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function formatTimeRange(startAt, endAt) {
  return `${formatDateTime(startAt)} - ${formatDateTime(endAt).slice(11)}`;
}

function parseDateTime(value) {
  if (value instanceof Date) {
    return value;
  }
  if (typeof value !== 'string') {
    return new Date(value);
  }
  const normalized = value.replace('T', ' ').split('.')[0];
  const parts = normalized.split(' ');
  const dateParts = (parts[0] || '').split('-').map(Number);
  const timeParts = (parts[1] || '00:00:00').split(':').map(Number);
  return new Date(
    dateParts[0],
    (dateParts[1] || 1) - 1,
    dateParts[2] || 1,
    timeParts[0] || 0,
    timeParts[1] || 0,
    timeParts[2] || 0
  );
}

function statusText(status) {
  const map = {
    SUBMITTED: '已提交',
    RISK_REVIEW: '风险审核',
    COUNSELOR_REVIEW: '咨询师审核',
    ADMIN_REVIEW: '后台审核',
    CONFIRMED: '已确认',
    CANCELED_BY_STUDENT: '已取消',
    CANCELED_BY_COUNSELOR: '咨询师取消',
    CANCELED_BY_ADMIN: '管理员取消',
    CHECKED_IN: '已到访',
    NO_SHOW: '未到访',
    COMPLETED: '已完成',
    REFERRED: '已转介',
    CLOSED: '已关闭'
  };
  return map[status] || status || '';
}

function statusTagType(status) {
  if (!status) {
    return 'neutral';
  }
  if (status.indexOf('CANCELED') === 0 || status === 'CLOSED' || status === 'NO_SHOW') {
    return 'neutral';
  }
  if (status === 'RISK_REVIEW' || status === 'REFERRED') {
    return 'warning';
  }
  if (status === 'CONFIRMED' || status === 'COMPLETED') {
    return '';
  }
  return 'neutral';
}

function urgencyText(value) {
  const map = {
    LOW: '一般',
    MEDIUM: '较急',
    HIGH: '紧急'
  };
  return map[value] || value || '-';
}

function riskLevelText(value) {
  const map = {
    LOW: '普通风险',
    MEDIUM: '中等风险',
    HIGH: '高风险'
  };
  return map[value] || value || '-';
}

module.exports = {
  addDays,
  toDateString,
  formatDateTime,
  formatTimeRange,
  statusText,
  statusTagType,
  urgencyText,
  riskLevelText
};
