import { api, type ClassSession, type DropoutAlert } from '../api.ts';
import { context } from '../context.ts';
import { empty, errorText, link, message, pageHeader, riskBadge, row, rows, section } from '../design-system/ui.ts';
import { el } from '../dom.ts';
import { formatDate, formatLongDay, formatTime, today } from '../format.ts';

export async function renderHome(main: HTMLElement): Promise<void> {
  const firstName = context.profile.fullName.split(' ')[0];
  const classes = el('div', {}, el('p', { role: 'status' }, 'Carregando…'));
  const alerts = el('div', {}, el('p', { role: 'status' }, 'Carregando…'));
  main.replaceChildren(
    pageHeader(`Olá, ${firstName}`, formatLongDay(today())),
    section('Aulas de hoje', classes),
    section('Risco de evasão', alerts),
  );

  const date = today();
  try {
    const [sessions, own] = await Promise.all([api.listClassSessions(date, date), api.listActivities()]);
    // Volunteers see only the classes they take attendance for.
    const ownIds = new Set(own.map((activity) => activity.id));
    const mine = sessions.filter((session) => session.status === 'ACTIVE' && ownIds.has(session.activityId));
    const none = context.manages ? 'Nenhuma aula hoje.' : 'Nenhuma aula sua hoje.';
    classes.replaceChildren(mine.length === 0 ? empty(none) : rows(...mine.map(sessionRow)));
  } catch (cause) {
    classes.replaceChildren(message('error', errorText(cause)));
  }

  try {
    const list = await api.listAlerts();
    alerts.replaceChildren(
      list.length === 0
        ? empty('Nenhum beneficiário com 3 ou mais faltas seguidas.')
        : el(
            'div',
            { class: 'stack' },
            el('p', { class: 'row__meta' }, 'Quem faltou 3 vezes ou mais seguidas. Ligue para a família.'),
            rows(...list.map(alertRow)),
          ),
    );
  } catch (cause) {
    alerts.replaceChildren(message('error', errorText(cause)));
  }
}

function sessionRow(session: ClassSession): HTMLLIElement {
  return row(
    [
      el('span', { class: 'row__title' }, session.title),
      el('span', { class: 'row__meta' }, `${formatTime(session.startTime)} às ${formatTime(session.endTime)}`),
    ],
    [link('Fazer chamada', `#/turmas/${session.activityId}`, 'primary')],
  );
}

function alertRow(alert: DropoutAlert): HTMLLIElement {
  return row(
    [
      el('span', { class: 'row__title' }, alert.beneficiaryName, ' ', riskBadge()),
      el(
        'span',
        { class: 'row__meta' },
        `${alert.consecutiveAbsences} faltas seguidas em ${alert.activityTitle}, última chamada em ${formatDate(alert.lastAttendanceDate)}`,
      ),
    ],
    [
      el('a', { class: 'button button--primary button--small', href: `tel:${alert.phoneNumber}` }, `Ligar ${alert.phoneNumber}`),
      link('Ver turma', `#/turmas/${alert.activityId}`),
    ],
  );
}
