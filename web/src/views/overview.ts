import {
  api,
  type AssistanceItem,
  type Delivery,
  type DropoutAlert,
  type ProgramOverview,
  type ZeloEvent,
} from '../api.ts';
import { context } from '../context.ts';
import { icon } from '../design-system/icons.ts';
import {
  avatar,
  badge,
  button,
  empty,
  errorText,
  link,
  loading,
  message,
  openDialog,
  pageHeader,
  row,
  rows,
  section,
} from '../design-system/ui.ts';
import { el } from '../dom.ts';
import {
  addDays,
  formatDate,
  formatLongDay,
  formatShort,
  formatTimeOfInstant,
  formatWeekday,
  instantToIsoDate,
  monthRange,
  parseDate,
  today,
} from '../format.ts';

const LIST_LIMIT = 5;

const UPCOMING_DAYS = 30;

// Home: the NGO at a glance for the current month, with the dropout risk on top.
export function renderOverview(main: HTMLElement): void {
  const firstName = context.profile.fullName.split(' ')[0];
  const now = today();
  // The month so far: classes after today are not due yet.
  const period = { from: monthRange(now).from, to: now };

  const strip = el('div', {});
  const stats = el('div', {}, loading());
  const attendance = el('div', {}, loading());
  const pending = el('div', {}, loading());
  const events = el('div', {}, loading());
  main.replaceChildren(
    pageHeader(`Olá, ${firstName}`, formatLongDay(now)),
    strip,
    stats,
    section('Frequência por turma', attendance),
    section('Chamadas pendentes', pending),
    section('Próximos eventos', events),
  );

  const overview = api.getProgramOverview(period.from, period.to);
  void renderRiskStrip(strip);
  void renderStats(stats, overview);
  void renderAttendance(attendance, overview);
  void renderPending(pending, overview);
  void renderEvents(events, now);
  if (context.manages) {
    const assistance = el('div', {}, loading());
    main.append(section('Assistência no mês', assistance));
    void renderAssistance(assistance, period);
  }
}

async function renderRiskStrip(strip: HTMLElement): Promise<void> {
  let alerts: DropoutAlert[];
  try {
    alerts = await api.listAlerts();
  } catch {
    return;
  }
  if (alerts.length === 0) {
    return;
  }
  // A child missing classes in two activities is one name, with both activities in the details.
  const byBeneficiary = new Map<number, DropoutAlert[]>();
  alerts.forEach((alert) =>
    byBeneficiary.set(alert.beneficiaryId, [...(byBeneficiary.get(alert.beneficiaryId) ?? []), alert]),
  );
  const count = byBeneficiary.size;
  strip.replaceChildren(
    el(
      'div',
      { class: 'risk-strip', role: 'status' },
      icon('alert'),
      el(
        'p',
        { class: 'risk-strip__text' },
        el('strong', {}, `${count} ${count === 1 ? 'criança em risco' : 'crianças em risco'} de evasão.`),
        ' Faltaram 3 vezes ou mais seguidas. Toque no nome para ver os dados.',
      ),
      el(
        'ul',
        { class: 'risk-strip__names' },
        ...[...byBeneficiary.values()].map((items) => {
          const name = button(items[0].beneficiaryName, () => openRiskDetails(items), { small: true });
          name.prepend(avatar(items[0].beneficiaryName));
          name.classList.add('risk-name');
          return el('li', {}, name);
        }),
      ),
    ),
  );
}

// For consulting only: the phone is shown as text, without a call button.
function openRiskDetails(items: DropoutAlert[]): void {
  const first = items[0];
  openDialog({
    title: first.beneficiaryName,
    icon: 'alert',
    tone: 'warning',
    body: [
      el(
        'dl',
        { class: 'facts facts--plain' },
        ...items.flatMap((alert) => [
          el('dt', {}, alert.activityTitle),
          el(
            'dd',
            {},
            `${alert.consecutiveAbsences} faltas seguidas, a última em ${formatDate(alert.lastAttendanceDate)}`,
          ),
        ]),
        el('dt', {}, 'Telefone'),
        el('dd', {}, first.phoneNumber),
      ),
    ],
    actions: (close) => [
      link('Abrir cadastro', `#/beneficiarios/${first.beneficiaryId}`),
      button('Fechar', close, { variant: 'primary' }),
    ],
  });
}

async function renderStats(container: HTMLElement, overview: Promise<ProgramOverview>): Promise<void> {
  let program: ProgramOverview;
  let beneficiaries: number;
  try {
    [program, beneficiaries] = await Promise.all([
      overview,
      api.searchBeneficiaries().then((list) => list.length),
    ]);
  } catch (cause) {
    container.replaceChildren(message('error', errorText(cause)));
    return;
  }
  const activeActivities = program.activities.filter((activity) => activity.status === 'ACTIVE').length;
  container.replaceChildren(
    el(
      'ul',
      { class: 'stat-grid' },
      stat(
        'Frequência no mês',
        program.attendanceRate === null ? 'Sem chamadas' : `${program.attendanceRate}%`,
        'Presenças nas chamadas do mês',
      ),
      stat('Beneficiários ativos', String(beneficiaries), 'Cadastros com situação ativa'),
      stat(
        'Turmas ativas',
        String(activeActivities),
        `${program.activeEnrollments} ${program.activeEnrollments === 1 ? 'aluno cursando' : 'alunos cursando'}`,
      ),
      stat('Saídas no mês', String(program.leftEnrollments), 'Alunos que saíram de uma turma'),
    ),
  );
}

function stat(label: string, value: string, hint: string): HTMLLIElement {
  return el(
    'li',
    { class: 'stat' },
    el('p', { class: 'stat__label' }, label),
    el('p', { class: 'stat__value' }, value),
    el('p', { class: 'stat__hint' }, hint),
  );
}

async function renderAttendance(container: HTMLElement, overview: Promise<ProgramOverview>): Promise<void> {
  let program: ProgramOverview;
  try {
    program = await overview;
  } catch (cause) {
    container.replaceChildren(message('error', errorText(cause)));
    return;
  }
  if (program.activities.length === 0) {
    container.replaceChildren(empty('Nenhuma turma neste mês.'));
    return;
  }
  // The percentage is always written next to the bar, so the bar is decoration for screen readers.
  container.replaceChildren(
    el(
      'ul',
      { class: 'rate-list' },
      ...program.activities.map((activity) =>
        el(
          'li',
          { class: 'rate-list__item' },
          el('a', { class: 'rate-list__title', href: `#/turmas/${activity.activityId}` }, activity.title),
          el(
            'span',
            { class: 'rate-list__value' },
            activity.attendanceRate === null ? 'Sem chamada' : `${activity.attendanceRate}%`,
          ),
          progressBar(activity.attendanceRate ?? 0),
        ),
      ),
    ),
  );
}

function progressBar(percent: number): HTMLElement {
  const fill = el('span', { class: 'progress-bar__fill' });
  fill.style.width = `${percent}%`;
  return el('span', { class: 'progress-bar', 'aria-hidden': 'true' }, fill);
}

async function renderPending(container: HTMLElement, overview: Promise<ProgramOverview>): Promise<void> {
  let program: ProgramOverview;
  try {
    program = await overview;
  } catch (cause) {
    container.replaceChildren(message('error', errorText(cause)));
    return;
  }
  const calls = program.pendingRollCalls;
  if (calls.length === 0) {
    container.replaceChildren(message('success', 'Todas as chamadas do mês estão em dia.'));
    return;
  }
  // Oldest first: the class longest without a roll call needs it most.
  container.replaceChildren(
    rows(
      ...calls.slice(0, LIST_LIMIT).map((call) =>
        row(
          [
            el('span', { class: 'row__title' }, call.title),
            el('span', { class: 'row__meta' }, `Aula de ${formatWeekday(call.date)}, ${formatShort(call.date)}`),
          ],
          [link('Fazer chamada', `#/turmas/${call.activityId}`, 'secondary', { small: true })],
        ),
      ),
    ),
    ...(calls.length > LIST_LIMIT
      ? [el('p', { class: 'row__meta' }, `E mais ${calls.length - LIST_LIMIT} chamadas pendentes neste mês.`)]
      : []),
  );
}

async function renderEvents(container: HTMLElement, now: string): Promise<void> {
  const end = parseDate(addDays(now, UPCOMING_DAYS));
  end.setHours(23, 59, 59, 999);
  let upcoming: ZeloEvent[];
  try {
    upcoming = (await api.listEvents(new Date().toISOString(), end.toISOString())).filter(
      (event) => event.status === 'SCHEDULED',
    );
  } catch (cause) {
    container.replaceChildren(message('error', errorText(cause)));
    return;
  }
  if (upcoming.length === 0) {
    container.replaceChildren(empty(`Nenhum evento nos próximos ${UPCOMING_DAYS} dias.`));
    return;
  }
  container.replaceChildren(
    rows(
      ...upcoming.slice(0, LIST_LIMIT).map((event) => {
        const date = instantToIsoDate(event.eventDate);
        return row(
          [
            el('a', { class: 'row__title', href: `#/eventos/${event.id}` }, event.title),
            el(
              'span',
              { class: 'row__meta' },
              `${formatWeekday(date)}, ${formatShort(date)} às ${formatTimeOfInstant(event.eventDate)}`
                + (event.location ? ` · ${event.location}` : ''),
            ),
          ],
        );
      }),
    ),
  );
}

async function renderAssistance(container: HTMLElement, period: { from: string; to: string }): Promise<void> {
  let deliveries: Delivery[];
  let items: AssistanceItem[];
  try {
    [deliveries, items] = await Promise.all([api.listDeliveries(period.from, period.to), api.listItems()]);
  } catch (cause) {
    container.replaceChildren(message('error', errorText(cause)));
    return;
  }
  const delivered = new Map<string, number>();
  deliveries.forEach((delivery) =>
    delivered.set(delivery.itemName, (delivered.get(delivery.itemName) ?? 0) + delivery.quantity),
  );
  const families = new Set(deliveries.map((delivery) => delivery.beneficiaryId)).size;
  const outOfStock = items.filter((item) => item.active && item.balance <= 0);

  container.replaceChildren(
    el(
      'ul',
      { class: 'stat-grid' },
      stat('Entregas no mês', String(deliveries.length), `Para ${families} ${families === 1 ? 'beneficiário' : 'beneficiários'}`),
    ),
    ...(delivered.size > 0
      ? [
          rows(
            ...[...delivered.entries()]
              .sort(([a], [b]) => a.localeCompare(b, 'pt-BR'))
              .map(([name, quantity]) =>
                row([el('span', { class: 'row__title' }, name)], [el('span', { class: 'row__meta' }, `${quantity} entregues`)]),
              ),
          ),
        ]
      : []),
    ...(outOfStock.length > 0
      ? [
          rows(
            ...outOfStock.map((item) =>
              row(
                [el('span', { class: 'row__title' }, item.name), el('span', { class: 'row__meta' }, `Saldo: ${item.balance}`)],
                [badge('risk', '⚠', 'Sem saldo')],
              ),
            ),
          ),
        ]
      : []),
  );
}
