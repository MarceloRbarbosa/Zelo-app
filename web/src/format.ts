// Dates travel as ISO strings (yyyy-mm-dd); the team reads them in pt-BR.
import type { DayOfWeek } from './api.ts';

export function isoDate(date: Date): string {
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${date.getFullYear()}-${month}-${day}`;
}

export function today(): string {
  return isoDate(new Date());
}

export function parseDate(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function addDays(iso: string, days: number): string {
  const date = parseDate(iso);
  date.setDate(date.getDate() + days);
  return isoDate(date);
}

export function monthRange(iso: string): { from: string; to: string } {
  const date = parseDate(iso);
  return {
    from: isoDate(new Date(date.getFullYear(), date.getMonth(), 1)),
    to: isoDate(new Date(date.getFullYear(), date.getMonth() + 1, 0)),
  };
}

export function addMonths(iso: string, months: number): string {
  const date = parseDate(iso);
  return isoDate(new Date(date.getFullYear(), date.getMonth() + months, 1));
}

const dateFormat = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
const shortFormat = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit' });
const weekdayFormat = new Intl.DateTimeFormat('pt-BR', { weekday: 'short' });
const longDayFormat = new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long' });
const monthFormat = new Intl.DateTimeFormat('pt-BR', { month: 'long', year: 'numeric' });
const dateTimeFormat = new Intl.DateTimeFormat('pt-BR', {
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
});
const timeFormat = new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' });

export const formatDate = (iso: string) => dateFormat.format(parseDate(iso));
export const formatShort = (iso: string) => shortFormat.format(parseDate(iso));
export const formatWeekday = (iso: string) => weekdayFormat.format(parseDate(iso)).replace('.', '');
export const formatLongDay = (iso: string) => {
  const text = longDayFormat.format(parseDate(iso));
  return text.charAt(0).toUpperCase() + text.slice(1);
};
export const formatMonth = (iso: string) => {
  const text = monthFormat.format(parseDate(iso));
  return text.charAt(0).toUpperCase() + text.slice(1);
};
export const formatDateTime = (instant: string) => dateTimeFormat.format(new Date(instant));
export const formatTimeOfInstant = (instant: string) => timeFormat.format(new Date(instant));
export const formatTime = (time: string) => time.slice(0, 5);

export const DAYS: { value: DayOfWeek; label: string; short: string }[] = [
  { value: 'MONDAY', label: 'Segunda', short: 'Seg' },
  { value: 'TUESDAY', label: 'Terça', short: 'Ter' },
  { value: 'WEDNESDAY', label: 'Quarta', short: 'Qua' },
  { value: 'THURSDAY', label: 'Quinta', short: 'Qui' },
  { value: 'FRIDAY', label: 'Sexta', short: 'Sex' },
  { value: 'SATURDAY', label: 'Sábado', short: 'Sáb' },
  { value: 'SUNDAY', label: 'Domingo', short: 'Dom' },
];

export function dayLabel(day: DayOfWeek): string {
  return DAYS.find((item) => item.value === day)?.label ?? day;
}

// Local date-time from <input type="datetime-local"> to an instant with the device's offset, and back.
export function localInputToInstant(value: string): string {
  return new Date(value).toISOString();
}

export function instantToLocalInput(instant: string): string {
  const date = new Date(instant);
  const hours = String(date.getHours()).padStart(2, '0');
  const minutes = String(date.getMinutes()).padStart(2, '0');
  return `${isoDate(date)}T${hours}:${minutes}`;
}

export function instantToIsoDate(instant: string): string {
  return isoDate(new Date(instant));
}
