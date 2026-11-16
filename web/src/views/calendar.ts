import { api, type ClassSession, type ZeloEvent } from '../api.ts';
import { button, empty, errorText, loading, message, pageHeader } from '../design-system/ui.ts';
import { el } from '../dom.ts';
import {
  addMonths,
  formatLongDay,
  formatMonth,
  formatTime,
  formatTimeOfInstant,
  instantToIsoDate,
  monthRange,
  parseDate,
  today,
} from '../format.ts';
import { eventStatusBadge } from './events.ts';

interface Entry {
  date: string;
  time: string;
  node: HTMLElement;
}

// Agenda of the month: every class generated from the activities' schedules and every event.
export async function renderCalendar(main: HTMLElement, month = today()): Promise<void> {
  const agenda = el('div', {}, loading());
  main.replaceChildren(
    pageHeader('Calendário', 'Aulas de todas as turmas e eventos da ONG.'),
    el(
      'div',
      { class: 'diary-toolbar' },
      el('h2', { class: 'diary-toolbar__month' }, formatMonth(month)),
      el(
        'div',
        { class: 'row__actions' },
        button('Mês anterior', () => void renderCalendar(main, addMonths(month, -1)), { small: true }),
        button('Hoje', () => void renderCalendar(main, today()), { small: true }),
        button('Próximo mês', () => void renderCalendar(main, addMonths(month, 1)), { small: true }),
      ),
    ),
    agenda,
  );

  const { from, to } = monthRange(month);
  const end = parseDate(to);
  end.setHours(23, 59, 59, 999);
  let sessions: ClassSession[];
  let events: ZeloEvent[];
  try {
    [sessions, events] = await Promise.all([
      api.listClassSessions(from, to),
      api.listEvents(parseDate(from).toISOString(), end.toISOString()),
    ]);
  } catch (cause) {
    agenda.replaceChildren(message('error', errorText(cause)));
    return;
  }

  const entries: Entry[] = [
    ...sessions.map((session) => ({
      date: session.date,
      time: formatTime(session.startTime),
      node: el(
        'li',
        { class: 'agenda__item agenda__item--class' },
        el('span', { class: 'agenda__time' }, `${formatTime(session.startTime)}–${formatTime(session.endTime)}`),
        el('a', { href: `#/turmas/${session.activityId}` }, session.title),
        el('span', { class: 'agenda__kind' }, 'Aula'),
      ),
    })),
    ...events.map((event) => ({
      date: instantToIsoDate(event.eventDate),
      time: formatTimeOfInstant(event.eventDate),
      node: el(
        'li',
        { class: 'agenda__item agenda__item--event' },
        el('span', { class: 'agenda__time' }, formatTimeOfInstant(event.eventDate)),
        el('a', { href: `#/eventos/${event.id}` }, event.title),
        eventStatusBadge(event),
      ),
    })),
  ].sort((a, b) => (a.date + a.time).localeCompare(b.date + b.time));

  if (entries.length === 0) {
    agenda.replaceChildren(empty('Nenhuma aula ou evento neste mês.'));
    return;
  }
  const days = new Map<string, Entry[]>();
  for (const entry of entries) {
    days.set(entry.date, [...(days.get(entry.date) ?? []), entry]);
  }
  const now = today();
  agenda.replaceChildren(
    el(
      'ol',
      { class: 'agenda' },
      ...[...days.entries()].map(([date, items]) => {
        const attributes: Record<string, string> = { class: 'agenda__day' };
        if (date === now) {
          attributes['aria-current'] = 'date';
        }
        return el(
          'li',
          attributes,
          el('h3', { class: 'agenda__date' }, formatLongDay(date), ...(date === now ? [el('span', { class: 'agenda__today' }, 'Hoje')] : [])),
          el('ul', { class: 'agenda__items' }, ...items.map((item) => item.node)),
        );
      }),
    ),
  );
}
