import { api, type Activity, type ClassSession, type DropoutAlert } from '../api.ts';
import { context } from '../context.ts';
import {
  avatar,
  badge,
  errorText,
  link,
  loading,
  message,
  pageHeader,
  riskBadge,
  stat,
} from '../design-system/ui.ts';
import { el } from '../dom.ts';
import { formatDate, formatLongDay, formatTime, today } from '../format.ts';

export async function renderHome(main: HTMLElement): Promise<void> {
  const firstName = context.profile.fullName.split(' ')[0];
  const create = context.manages ? link('Nova turma', '#/turmas/nova', 'primary', { icon: 'plus' }) : null;
  const stats = el('div', { class: 'stats' }, loading());
  const alerts = el('div', {}, loading());
  const classes = el('div', {}, loading());
  main.replaceChildren(
    pageHeader(`Olá, ${firstName}`, formatLongDay(today()), ...(create ? [create] : [])),
    stats,
    el(
      'div',
      { class: 'home-grid' },
      card('Risco de evasão', 'Quem faltou 3 vezes ou mais seguidas. Ligue para a família.', alerts),
      card('Aulas de hoje', 'Abra a turma para fazer a chamada.', classes),
    ),
  );

  const date = today();
  let sessions: ClassSession[] = [];
  let mine: Activity[] = [];
  let alertList: DropoutAlert[] = [];
  try {
    [sessions, mine, alertList] = await Promise.all([
      api.listClassSessions(date, date),
      api.listActivities(),
      api.listAlerts(),
    ]);
  } catch (cause) {
    stats.replaceChildren(message('error', errorText(cause)));
    alerts.replaceChildren();
    classes.replaceChildren();
    return;
  }

  // Volunteers see only the classes they take attendance for.
  const ownIds = new Set(mine.map((activity) => activity.id));
  const todays = sessions.filter((session) => session.status === 'ACTIVE' && ownIds.has(session.activityId));
  // A class is done when its diary for today already has marks.
  const done = new Set<number>();
  await Promise.all(
    [...new Set(todays.map((session) => session.activityId))].map(async (activityId) => {
      const diary = await api.getDiary(activityId, date, date).catch(() => null);
      if (diary?.students.some((student) => student.marks.length > 0)) {
        done.add(activityId);
      }
    }),
  );
  const pending = todays.filter((session) => !done.has(session.activityId)).length;
  const students = await api
    .searchBeneficiaries({})
    .then((list) => list.length)
    .catch(() => null);

  stats.replaceChildren(
    stat('book', 'Aulas hoje', String(todays.length), pending === 0 ? 'Nenhuma chamada pendente' : `${pending} ${pending === 1 ? 'chamada pendente' : 'chamadas pendentes'}`),
    stat('alert', 'Em risco de evasão', String(alertList.length), '3 ou mais faltas seguidas'),
    stat('people', 'Beneficiários ativos', students === null ? '–' : String(students), `${mine.length} ${mine.length === 1 ? 'turma em andamento' : 'turmas em andamento'}`),
  );

  alerts.replaceChildren(
    alertList.length === 0
      ? el('p', { class: 'card__empty' }, 'Nenhum beneficiário em risco agora.')
      : el('ul', { class: 'card__list' }, ...alertList.map(alertItem)),
  );
  classes.replaceChildren(
    todays.length === 0
      ? el('p', { class: 'card__empty' }, context.manages ? 'Nenhuma aula hoje.' : 'Nenhuma aula sua hoje.')
      : el('ul', { class: 'card__list' }, ...todays.map((session) => sessionItem(session, done.has(session.activityId)))),
  );
}

function card(title: string, subtitle: string, body: HTMLElement): HTMLElement {
  return el(
    'section',
    { class: 'card' },
    el('header', { class: 'card__head' }, el('h2', {}, title), el('p', { class: 'row__meta' }, subtitle)),
    body,
  );
}

function alertItem(alert: DropoutAlert): HTMLLIElement {
  return el(
    'li',
    { class: 'card__item' },
    avatar(alert.beneficiaryName),
    el(
      'div',
      { class: 'row__main' },
      el('span', { class: 'row__title' }, alert.beneficiaryName),
      el(
        'span',
        { class: 'row__meta' },
        `${alert.activityTitle}, ${alert.consecutiveAbsences} faltas seguidas, última chamada em ${formatDate(alert.lastAttendanceDate)}`,
      ),
    ),
    riskBadge(),
    el(
      'a',
      { class: 'button button--secondary button--small', href: `tel:${alert.phoneNumber}`, 'aria-label': `Ligar para ${alert.phoneNumber}` },
      'Ligar',
    ),
  );
}

function sessionItem(session: ClassSession, done: boolean): HTMLLIElement {
  return el(
    'li',
    { class: 'card__item' },
    el('span', { class: 'card__time' }, formatTime(session.startTime)),
    el(
      'div',
      { class: 'row__main' },
      el('a', { class: 'row__title', href: `#/turmas/${session.activityId}` }, session.title),
      el('span', { class: 'row__meta' }, `${formatTime(session.startTime)} às ${formatTime(session.endTime)}`),
    ),
    done
      ? badge('present', '✓', 'Chamada feita')
      : link('Fazer chamada', `#/turmas/${session.activityId}`, 'primary', { small: true }),
  );
}
