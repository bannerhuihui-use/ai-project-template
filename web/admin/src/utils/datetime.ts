import dayjs from 'dayjs';

/** 兼容后端 LocalDateTime 多种序列化：字符串 / ISO / 数组。 */
export function formatDateTime(value: unknown): string {
  if (value == null || value === '') return '';

  if (Array.isArray(value)) {
    const [y, m, d, h = 0, min = 0, s = 0] = value as number[];
    if (y == null || m == null || d == null) return '';
    const parsed = dayjs(new Date(y, m - 1, d, h, min, s));
    return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : '';
  }

  const parsed = dayjs(String(value));
  return parsed.isValid() ? parsed.format('YYYY-MM-DD HH:mm:ss') : String(value);
}

/** 表格时间列：日期 + 时分秒两行展示。 */
export function renderDateTimeCell(value: unknown) {
  const text = formatDateTime(value);
  if (!text) return null;
  const [date, time] = text.split(' ');
  return { date, time };
}
