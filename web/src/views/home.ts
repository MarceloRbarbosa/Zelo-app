import { api, type ClassSession, type ZeloEvent } from '../api.ts';
import { context } from '../context.ts';
import { icon } from '../design-system/icons.ts';
import { button, errorText, link, loading, message, pageHeader } from '../design-system/ui.ts';
import { el } from '../dom.ts';
import {
  addDays,
  addMonths,
  formatLongDay,
  formatMonth,
  formatShort,
  formatTime,
  formatTimeOfInstant,
  instantToIsoDate,
  monthRange,
  parseDate,
  today,
} from '../format.ts';

type View = 'month' | 'week';

// Each activity keeps the same color everywhere in the agenda; events use the sunflower.
const ACTIVITY_COLORS = 6;
const WEEKDAYS = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];

interface AgendaItem {
  date: string;
  time: string;
  label: string;
  href: string;
  kind: 'class' | 'event';
  color: number;
  canceled: boolean;
}

function startOfWeek(iso: string): string {
  const weekday = (parseDate(iso).getDay() + 6) % 7;
  return addDays(iso, -weekday);
}

export async function renderHome(main: HTMLElement): Promise<void> {
  const actions: HTMLElement[] = [];
  if (context.manages) {
    actions.push(link('Novo evento', '#/eventos/novo', 'secondary', { icon: 'plus' }));
  }
  const todayAction = el('span', {});
  actions.push(todayAction);
  const agenda = el('section', { class: 'agenda-card', 'aria-label': 'Agenda da ONG' }, loading());
  main.replaceChildren(pageHeader('Agenda', formatLongDay(today()), ...actions), agenda);

  const view: View = window.matchMedia('(max-width: 719px)').matches ? 'week' : 'month';
  void renderAgenda(agenda, view, today());
  void renderTodayAction(todayAction);
}

// Shortcut to the first class of the day whose attendance is still missing.
async function renderTodayAction(slot: HTMLElement): Promise<void> {
  const date = today();
  try {
    const [sessions, mine] = await Promise.all([api.listClassSessions(date, date), api.listActivities()]);
    const ownIds = new Set(mine.map((activity) => activity.id));
    for (const session of sessions.filter((item) => item.status === 'ACTIVE' && ownIds.has(item.activityId))) {
      const diary = await api.getDiary(session.activityId, date, date).catch(() => null);
      if (!diary?.students.some((student) => student.marks.length > 0)) {
        slot.replaceChildren(link('Fazer chamada de hoje', `#/turmas/${session.activityId}`, 'primary'));
        return;
      }
    }
  } catch {
    // The shortcut is optional; the agenda still works without it.
  }
}

async function renderAgenda(container: HTMLElement, view: View, anchor: string): Promise<void> {
  const range =
    view === 'month'
      ? { from: startOfWeek(monthRange(anchor).from), to: addDays(startOfWeek(monthRange(anchor).to), 6) }
      : { from: startOfWeek(anchor), to: addDays(startOfWeek(anchor), 6) };

  const go = (nextView: View, nextAnchor: string) => void renderAgenda(container, nextView, nextAnchor);
  const step = (direction: number) =>
    view === 'month' ? addMonths(anchor, direction) : addDays(startOfWeek(anchor), 7 * direction);
  const title =
    view === 'month' ? formatMonth(anchor) : `${formatShort(range.from)} a ${formatShort(range.to)}`;
  const toggle = el(
    'div',
    { class: 'segmented', role: 'group', 'aria-label': 'Visualização' },
    ...(['month', 'week'] as View[]).map((option) => {
      const item = el(
        'button',
        { type: 'button', class: 'segmented__item', 'aria-pressed': String(option === view) },
        option === 'month' ? 'Mês' : 'Semana',
      );
      item.addEventListener('click', () => go(option, anchor));
      return item;
    }),
  );
  const head = el(
    'div',
    { class: 'agenda-card__head' },
    el(
      'div',
      { class: 'agenda-card__nav' },
      navButton('chevronLeft', view === 'month' ? 'Mês anterior' : 'Semana anterior', () => go(view, step(-1))),
      navButton('chevronRight', view === 'month' ? 'Próximo mês' : 'Próxima semana', () => go(view, step(1))),
      el('h2', { class: 'agenda-card__title' }, title),
      button('Hoje', () => go(view, today()), { small: true }),
    ),
    toggle,
  );
  container.replaceChildren(head, loading());

  let sessions: ClassSession[];
  let events: ZeloEvent[];
  try {
    const end = parseDate(range.to);
    end.setHours(23, 59, 59, 999);
    [sessions, events] = await Promise.all([
      api.listClassSessions(range.from, range.to),
      api.listEvents(parseDate(range.from).toISOString(), end.toISOString()),
    ]);
  } catch (cause) {
    container.replaceChildren(head, message('error', errorText(cause)));
    return;
  }

  const items: AgendaItem[] = [
    ...sessions.map((session) => ({
      date: session.date,
      time: formatTime(session.startTime),
      label: session.title,
      href: `#/turmas/${session.activityId}`,
      kind: 'class' as const,
      color: session.activityId % ACTIVITY_COLORS,
      canceled: false,
    })),
    ...events.map((event) => ({
      date: instantToIsoDate(event.eventDate),
      time: formatTimeOfInstant(event.eventDate),
      label: event.title,
      href: `#/eventos/${event.id}`,
      kind: 'event' as const,
      color: 0,
      canceled: event.status === 'CANCELED',
    })),
  ].sort((a, b) => (a.date + a.time).localeCompare(b.date + b.time));
  const byDate = new Map<string, AgendaItem[]>();
  items.forEach((item) => byDate.set(item.date, [...(byDate.get(item.date) ?? []), item]));

  const days: string[] = [];
  for (let date = range.from; date <= range.to; date = addDays(date, 1)) {
    days.push(date);
  }
  const month = parseDate(anchor).getMonth();
  const now = today();
  const grid = el(
    'div',
    { class: `agenda-grid agenda-grid--${view}` },
    ...WEEKDAYS.map((day) => el('div', { class: 'agenda-grid__weekday', 'aria-hidden': 'true' }, day)),
    ...days.map((date) => {
      const attributes: Record<string, string> = { class: 'agenda-day' };
      if (view === 'month' && parseDate(date).getMonth() !== month) {
        attributes.class += ' agenda-day--outside';
      }
      if (date === now) {
        attributes['aria-current'] = 'date';
      }
      const dayItems = byDate.get(date) ?? [];
      const shown = view === 'month' ? dayItems.slice(0, 3) : dayItems;
      return el(
        'div',
        attributes,
        el(
          'p',
          { class: 'agenda-day__date' },
          el('span', { class: 'agenda-day__weekday' }, WEEKDAYS[(parseDate(date).getDay() + 6) % 7]),
          el('span', { class: 'agenda-day__number' }, String(parseDate(date).getDate())),
        ),
        ...shown.map(agendaChip),
        ...(dayItems.length > shown.length
          ? [el('p', { class: 'agenda-day__more' }, `+${dayItems.length - shown.length} mais`)]
          : []),
        ...(view === 'week' && dayItems.length === 0 ? [el('p', { class: 'agenda-day__empty' }, 'Sem atividades')] : []),
      );
    }),
  );

  const legend = new Map<string, number>();
  sessions.forEach((session) => legend.set(session.title, session.activityId % ACTIVITY_COLORS));
  container.replaceChildren(
    head,
    grid,
    el(
      'p',
      { class: 'agenda-legend' },
      ...[...legend.entries()].map(([label, color]) =>
        el('span', {}, el('i', { class: `agenda-legend__dot activity-color-${color}`, 'aria-hidden': 'true' }), label),
      ),
      el('span', {}, el('i', { class: 'agenda-legend__dot agenda-legend__dot--event', 'aria-hidden': 'true' }), 'Eventos'),
    ),
  );
}

function agendaChip(item: AgendaItem): HTMLAnchorElement {
  const classes = ['agenda-chip', item.kind === 'event' ? 'agenda-chip--event' : `activity-color-${item.color}`];
  if (item.canceled) {
    classes.push('agenda-chip--canceled');
  }
  const label = `${item.time} ${item.label}${item.canceled ? ' (cancelado)' : ''}`;
  return el('a', { class: classes.join(' '), href: item.href, title: label }, label);
}

function navButton(name: 'chevronLeft' | 'chevronRight', label: string, onClick: () => void): HTMLButtonElement {
  const element = el(
    'button',
    { type: 'button', class: 'button button--secondary button--small button--icon', 'aria-label': label, title: label },
    icon(name),
  );
  element.addEventListener('click', onClick);
  return element;
}
