import {
  api,
  type Activity,
  type ActivitySchedule,
  type DayOfWeek,
  type Diary,
  type DiaryStudent,
  type Enrollment,
  type StaffMember,
} from '../api.ts';
import { context } from '../context.ts';
import {
  avatar,
  badge,
  button,
  checkbox,
  empty,
  errorText,
  field,
  input,
  link,
  loading,
  message,
  pageHeader,
  riskBadge,
  row,
  rows,
  section,
  select,
  showFieldErrors,
  tabs,
  textarea,
} from '../design-system/ui.ts';
import { icon, type IconName } from '../design-system/icons.ts';
import { el } from '../dom.ts';
import {
  addMonths,
  DAYS,
  formatDate,
  formatMonth,
  formatShort,
  formatTime,
  formatWeekday,
  monthRange,
  today,
} from '../format.ts';
import { navigate } from '../router.ts';
import { beneficiaryPicker } from './picker.ts';

function scheduleText(schedules: ActivitySchedule[]): string {
  return schedules
    .map((schedule) => {
      const day = DAYS.find((item) => item.value === schedule.dayOfWeek)?.short ?? schedule.dayOfWeek;
      return `${day} ${formatTime(schedule.startTime)}–${formatTime(schedule.endTime)}`;
    })
    .join(', ');
}

// List

export async function renderActivities(main: HTMLElement, status: Activity['status'] = 'ACTIVE'): Promise<void> {
  const create = context.manages ? link('Nova turma', '#/turmas/nova', 'primary') : null;
  const subtitle = context.manages ? 'Turmas e oficinas da ONG.' : 'Turmas em que você é responsável.';
  const list = el('div', {}, loading());
  main.replaceChildren(
    pageHeader('Turmas', subtitle, ...(create ? [create] : [])),
    tabs(
      [
        { id: 'ACTIVE', label: 'Em andamento' },
        { id: 'FINISHED', label: 'Finalizadas' },
      ],
      status,
      (id) => void renderActivities(main, id as Activity['status']),
    ),
    list,
  );
  try {
    const activities = await api.listActivities(status);
    if (activities.length === 0) {
      list.replaceChildren(
        empty(
          status === 'ACTIVE' ? 'Nenhuma turma em andamento.' : 'Nenhuma turma finalizada.',
          status === 'ACTIVE' && create ? link('Cadastrar a primeira turma', '#/turmas/nova', 'primary') : undefined,
        ),
      );
      return;
    }
    list.replaceChildren(
      rows(
        ...activities.map((activity) =>
          row(
            [
              el('a', { class: 'row__title', href: `#/turmas/${activity.id}` }, activity.title),
              el('span', { class: 'row__meta' }, scheduleText(activity.schedules)),
              el(
                'span',
                { class: 'row__meta' },
                `Responsáveis: ${activity.staff.map((staff) => staff.fullName).join(', ')}`,
              ),
            ],
            [link('Abrir', `#/turmas/${activity.id}`)],
          ),
        ),
      ),
    );
  } catch (cause) {
    list.replaceChildren(message('error', errorText(cause)));
  }
}

// Create and edit

interface ScheduleRow {
  root: HTMLElement;
  read(): ActivitySchedule;
}

function scheduleRow(schedule: ActivitySchedule | null, onRemove: (row: ScheduleRow) => void): ScheduleRow {
  const day = field(
    'Dia',
    select(
      DAYS.map((item) => ({ value: item.value, label: item.label })),
      schedule?.dayOfWeek ?? 'MONDAY',
    ),
  );
  const start = field('Início', input({ type: 'time', value: schedule ? formatTime(schedule.startTime) : '14:00' }));
  const end = field('Fim', input({ type: 'time', value: schedule ? formatTime(schedule.endTime) : '15:00' }));
  const root = el('div', { class: 'schedule-row' }, day.root, start.root, end.root);
  const item: ScheduleRow = {
    root,
    read: () => ({
      dayOfWeek: day.control.value as DayOfWeek,
      startTime: start.control.value,
      endTime: end.control.value,
    }),
  };
  root.append(button('Remover', () => onRemove(item), { variant: 'ghost', small: true }));
  return item;
}

export async function renderActivityForm(main: HTMLElement, id?: number): Promise<void> {
  main.replaceChildren(loading());
  let activity: Activity | null = null;
  let staff: StaffMember[];
  try {
    [activity, staff] = await Promise.all([id ? api.getActivity(id) : Promise.resolve(null), api.listStaff()]);
  } catch (cause) {
    main.replaceChildren(message('error', errorText(cause, 'Turma não encontrada.')));
    return;
  }

  const title = field('Nome da turma', input({ maxlength: '100', value: activity?.title ?? '' }));
  const description = field('Descrição', textarea(activity?.description ?? ''), 'Opcional. Até 255 caracteres.');
  const startDate = field('Data de início', input({ type: 'date', value: activity?.startDate ?? today() }));

  const schedules: ScheduleRow[] = [];
  const scheduleList = el('div', { class: 'stack' });
  const scheduleError = el('p', { class: 'field__error' });
  const addSchedule = (schedule: ActivitySchedule | null) => {
    const item = scheduleRow(schedule, (removed) => {
      schedules.splice(schedules.indexOf(removed), 1);
      removed.root.remove();
    });
    schedules.push(item);
    scheduleList.append(item.root);
  };
  (activity?.schedules.length ? activity.schedules : [null]).forEach(addSchedule);

  // Blocked members are hidden unless they are already responsible for the activity.
  const selected = new Set(activity?.staff.map((member) => member.id) ?? []);
  const staffChecks = staff
    .filter((member) => member.status === 'ACTIVE' || selected.has(member.id))
    .map((member) => ({ id: member.id, ...checkbox(member.fullName, selected.has(member.id)) }));
  const staffError = el('p', { class: 'field__error' });

  const feedback = el('div', {});
  const save = button(activity ? 'Salvar alterações' : 'Criar turma', undefined, { variant: 'primary', type: 'submit' });
  const cancel = link('Cancelar', activity ? `#/turmas/${activity.id}` : '#/turmas', 'ghost');

  const form = el(
    'form',
    { class: 'form', novalidate: '' },
    title.root,
    description.root,
    startDate.root,
    el(
      'fieldset',
      {},
      el('legend', {}, 'Horários'),
      el('p', { class: 'field__hint' }, 'Os dias de aula do diário e do calendário saem destes horários.'),
      scheduleList,
      scheduleError,
      button('Adicionar horário', () => addSchedule(null), { small: true }),
    ),
    el(
      'fieldset',
      {},
      el('legend', {}, 'Responsáveis'),
      el('p', { class: 'field__hint' }, 'Quem faz a chamada. Escolha uma ou mais pessoas.'),
      ...staffChecks.map((check) => check.root),
      staffError,
    ),
    feedback,
    el('div', { class: 'form__actions' }, save, cancel),
  );

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    [title, startDate].forEach((item) => item.setError(''));
    scheduleError.textContent = '';
    staffError.textContent = '';
    const staffIds = staffChecks.filter((check) => check.input.checked).map((check) => check.id);
    let valid = true;
    if (!title.control.value.trim()) {
      title.setError('Dê um nome à turma.');
      valid = false;
    }
    if (schedules.length === 0) {
      scheduleError.textContent = 'Adicione pelo menos um horário.';
      valid = false;
    }
    if (staffIds.length === 0) {
      staffError.textContent = 'Escolha pelo menos um responsável.';
      valid = false;
    }
    if (!valid) {
      return;
    }
    save.disabled = true;
    const body = {
      title: title.control.value.trim(),
      description: description.control.value.trim() || null,
      startDate: startDate.control.value,
      schedules: schedules.map((item) => item.read()),
      staffIds,
    };
    try {
      const saved = activity ? await api.updateActivity(activity.id, body) : await api.createActivity(body);
      navigate(`/turmas/${saved.id}`);
    } catch (cause) {
      if (!showFieldErrors(cause, { title, startDate })) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
      save.disabled = false;
    }
  });

  main.replaceChildren(
    pageHeader(
      activity ? `Editar ${activity.title}` : 'Nova turma',
      activity ? undefined : 'Depois de criar, você já cai no diário da turma para matricular e fazer a chamada.',
    ),
    form,
  );
  title.control.focus();
}

// Activity page: attendance, diary and students

type Tab = 'chamada' | 'diario' | 'alunos';

export async function renderActivity(main: HTMLElement, id: number, tab?: Tab): Promise<void> {
  main.replaceChildren(loading());
  let activity: Activity;
  try {
    activity = await api.getActivity(id);
  } catch (cause) {
    main.replaceChildren(message('error', errorText(cause, 'Turma não encontrada.')));
    return;
  }
  const active = activity.status === 'ACTIVE';
  const current: Tab = tab ?? (active ? 'chamada' : 'diario');

  const actions: HTMLElement[] = [];
  const finishPanel = el('div', {});
  if (context.manages && active) {
    actions.push(
      link('Editar', `#/turmas/${activity.id}/editar`),
      button('Finalizar turma', () => finishPanel.replaceChildren(finishForm(activity, main)), { variant: 'danger' }),
    );
  }

  const status = active
    ? badge('present', '●', 'Em andamento')
    : badge('neutral', '■', `Finalizada em ${formatDate(activity.endDate ?? activity.startDate)}`);
  const details = el(
    'div',
    { class: 'activity-details' },
    status,
    el('p', {}, scheduleText(activity.schedules)),
    el('p', { class: 'row__meta' }, `Responsáveis: ${activity.staff.map((staff) => staff.fullName).join(', ')}`),
    el('p', { class: 'row__meta' }, `Início em ${formatDate(activity.startDate)}`),
  );

  const body = el('div', {});
  const tabItems = [
    ...(active ? [{ id: 'chamada', label: 'Chamada' }] : []),
    { id: 'diario', label: 'Diário' },
    { id: 'alunos', label: 'Alunos' },
  ];
  main.replaceChildren(
    pageHeader(activity.title, activity.description ?? undefined, ...actions),
    details,
    finishPanel,
    tabs(tabItems, current, (next) => void renderActivity(main, id, next as Tab)),
    body,
  );

  if (current === 'chamada') {
    await attendanceTab(body, activity);
  } else if (current === 'diario') {
    await diaryTab(body, activity, today());
  } else {
    await studentsTab(body, activity);
  }
}

function finishForm(activity: Activity, main: HTMLElement): HTMLElement {
  const endDate = field('Data de fim', input({ type: 'date', value: today(), min: activity.startDate }));
  const feedback = el('div', {});
  const confirm = button('Confirmar finalização', undefined, { variant: 'danger', type: 'submit' });
  const form = el(
    'form',
    { class: 'form panel finish-panel' },
    el('p', {}, 'A turma sai da chamada e o diário fica só para consulta. Não dá para desfazer.'),
    endDate.root,
    feedback,
    el('div', { class: 'form__actions' }, confirm),
  );
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    confirm.disabled = true;
    try {
      await api.finishActivity(activity.id, endDate.control.value);
      void renderActivity(main, activity.id, 'diario');
    } catch (cause) {
      feedback.replaceChildren(message('error', errorText(cause)));
      confirm.disabled = false;
    }
  });
  return form;
}

type Mark = 'present' | 'absent';

const MARK_LABEL: Record<Mark | 'unmarked', { icon: IconName; text: string }> = {
  present: { icon: 'check', text: 'Presente' },
  absent: { icon: 'x', text: 'Ausente' },
  unmarked: { icon: 'circle', text: 'Não marcado' },
};

async function attendanceTab(body: HTMLElement, activity: Activity, date = today()): Promise<void> {
  const dateField = field('Data da aula', input({ type: 'date', value: date, max: today(), min: activity.startDate }));
  dateField.control.addEventListener('change', () => void attendanceTab(body, activity, dateField.control.value));
  const list = el('div', {}, loading());
  body.replaceChildren(el('div', { class: 'attendance-date' }, dateField.root), list);

  let diary: Diary;
  try {
    diary = await api.getDiary(activity.id, date, date);
  } catch (cause) {
    list.replaceChildren(message('error', errorText(cause)));
    return;
  }

  // Students still enrolled, plus anyone who already has a mark on this date.
  const students = diary.students.filter(
    (student) => student.enrollmentStatus === 'ACTIVE' || student.marks.some((mark) => mark.date === date),
  );
  if (students.length === 0) {
    list.replaceChildren(empty('Nenhum aluno matriculado nesta turma. As matrículas ficam na aba Alunos.'));
    return;
  }

  const marks = new Map<number, Mark>();
  const notes = new Map<number, string>();
  for (const student of students) {
    const mark = student.marks.find((item) => item.date === date);
    if (mark) {
      marks.set(student.beneficiaryId, mark.present ? 'present' : 'absent');
      if (mark.notes) {
        notes.set(student.beneficiaryId, mark.notes);
      }
    }
  }
  const correcting = marks.size > 0;

  const progress = el('p', { class: 'progress', 'aria-live': 'polite' });
  const bar = el('span', { class: 'progress-bar__fill' });
  const feedback = el('div', {});
  const save = button(correcting ? 'Salvar correção' : 'Salvar chamada', undefined, { variant: 'primary' });
  const markRest = button('Restantes presentes', undefined, { small: true, icon: 'check' });

  const cards = students.map((student) => attendanceCard(student, marks, notes, () => update()));
  const update = () => {
    progress.textContent = `${marks.size} de ${students.length} marcados`;
    bar.style.width = `${Math.round((100 * marks.size) / students.length)}%`;
    save.disabled = marks.size < students.length;
    markRest.disabled = marks.size === students.length;
    feedback.replaceChildren();
  };
  // Marks only who is still unmarked, so absences already chosen are kept.
  markRest.addEventListener('click', () => {
    students
      .filter((student) => !marks.has(student.beneficiaryId))
      .forEach((student) => marks.set(student.beneficiaryId, 'present'));
    cards.forEach((card) => card.paint());
    update();
  });

  save.addEventListener('click', async () => {
    save.disabled = true;
    const batch = {
      activityId: activity.id,
      attendanceDate: date,
      entries: students.map((student) => ({
        beneficiaryId: student.beneficiaryId,
        present: marks.get(student.beneficiaryId) === 'present',
        notes: notes.get(student.beneficiaryId) || null,
      })),
    };
    try {
      const result = correcting ? await api.correctAttendance(batch) : await api.registerAttendance(batch);
      feedback.replaceChildren(
        message(
          'success',
          `${correcting ? 'Chamada corrigida' : 'Chamada salva'}: ${result.present} presentes e ${result.absent} ausentes.`,
        ),
      );
      save.textContent = 'Salvar correção';
    } catch (cause) {
      feedback.replaceChildren(message('error', errorText(cause)));
    } finally {
      save.disabled = false;
    }
  });

  list.replaceChildren(
    correcting
      ? message('info', `A chamada de ${formatDate(date)} já foi feita. Altere o que precisar e salve a correção.`)
      : el('span', {}),
    el(
      'div',
      { class: 'attendance-summary' },
      el('div', { class: 'progress-bar', 'aria-hidden': 'true' }, bar),
      el('div', { class: 'toolbar' }, progress, markRest),
    ),
    el('ul', { class: 'attendance-cards', 'aria-label': 'Alunos' }, ...cards.map((card) => card.root)),
    el('div', { class: 'footer-bar' }, feedback, save),
  );
  update();
}

function attendanceCard(
  student: DiaryStudent,
  marks: Map<number, Mark>,
  notes: Map<number, string>,
  onChange: () => void,
): { root: HTMLElement; paint: () => void } {
  const id = student.beneficiaryId;
  const state = el('span', { class: 'attendance-card__state' });
  const present = el(
    'button',
    { type: 'button', class: 'toggle toggle--present', 'aria-pressed': 'false', 'aria-label': 'Presente', title: 'Presente' },
    icon('check'),
  );
  const absent = el(
    'button',
    { type: 'button', class: 'toggle toggle--absent', 'aria-pressed': 'false', 'aria-label': 'Ausente', title: 'Ausente' },
    icon('x'),
  );
  const note = input({ maxlength: '255', value: notes.get(id) ?? '', 'aria-label': `Observação sobre ${student.fullName}` });
  note.classList.add('input');
  note.addEventListener('input', () => notes.set(id, note.value));
  const noteBox = el('details', { class: 'attendance-card__note' }, el('summary', {}, 'Observação'), note);
  if (notes.get(id)) {
    noteBox.open = true;
  }
  const info = el('div', { class: 'attendance-card__info' }, el('span', { class: 'attendance-card__name' }, student.fullName), state);
  if (student.dropoutRisk) {
    info.append(riskBadge());
  }
  const root = el(
    'li',
    { class: 'attendance-card' },
    el(
      'div',
      { class: 'attendance-card__header' },
      avatar(student.fullName),
      info,
      el('div', { class: 'attendance-card__actions', role: 'group', 'aria-label': `Presença de ${student.fullName}` }, present, absent),
    ),
    noteBox,
  );
  const paint = () => {
    const mark = marks.get(id);
    const label = MARK_LABEL[mark ?? 'unmarked'];
    root.dataset.state = mark ?? 'unmarked';
    state.replaceChildren(icon(label.icon), label.text);
    present.setAttribute('aria-pressed', String(mark === 'present'));
    absent.setAttribute('aria-pressed', String(mark === 'absent'));
  };
  const choose = (mark: Mark) => {
    marks.set(id, mark);
    paint();
    onChange();
  };
  present.addEventListener('click', () => choose('present'));
  absent.addEventListener('click', () => choose('absent'));
  paint();
  return { root, paint };
}

async function diaryTab(body: HTMLElement, activity: Activity, month: string): Promise<void> {
  const { from, to } = monthRange(month);
  const previous = iconButton('chevronLeft', 'Mês anterior', () => void diaryTab(body, activity, addMonths(month, -1)));
  const next = iconButton('chevronRight', 'Próximo mês', () => void diaryTab(body, activity, addMonths(month, 1)));
  const print = button('Imprimir', () => window.print(), { small: true, icon: 'print' });
  const grid = el('div', {}, loading());
  body.replaceChildren(
    el(
      'div',
      { class: 'diary-toolbar' },
      el('h2', { class: 'diary-toolbar__month' }, formatMonth(from)),
      el('div', { class: 'row__actions' }, previous, next, print),
    ),
    grid,
  );

  let diary: Diary;
  try {
    diary = await api.getDiary(activity.id, from, to);
  } catch (cause) {
    grid.replaceChildren(message('error', errorText(cause)));
    return;
  }
  if (diary.students.length === 0) {
    grid.replaceChildren(empty('Nenhum aluno matriculado nesta turma.'));
    return;
  }
  if (diary.dates.length === 0) {
    grid.replaceChildren(empty('Não há aula desta turma neste mês.'));
    return;
  }
  grid.replaceChildren(diaryTable(diary), diaryLegend());
}

// The class register: one row per student, one column per class date, like the paper book it replaces.
function diaryTable(diary: Diary): HTMLElement {
  const head = el(
    'tr',
    {},
    el('th', { scope: 'col', class: 'diary__student' }, 'Aluno'),
    el('th', { scope: 'col', class: 'diary__rate' }, 'Frequência'),
    ...diary.dates.map((date) =>
      el(
        'th',
        { scope: 'col', class: 'diary__date' },
        el('span', { class: 'diary__weekday' }, formatWeekday(date)),
        el('span', {}, formatShort(date)),
      ),
    ),
  );
  const lines = diary.students.map((student) => {
    const byDate = new Map(student.marks.map((mark) => [mark.date, mark]));
    const who = el('div', { class: 'diary__who' }, el('span', { class: 'diary__name' }, student.fullName));
    if (student.enrollmentStatus === 'LEFT') {
      who.append(badge('neutral', '↩', `Saiu em ${formatDate(student.leftAt ?? '')}`));
    }
    if (student.dropoutRisk) {
      who.append(riskBadge());
    }
    const name = el('th', { scope: 'row', class: 'diary__student' }, avatar(student.fullName), who);
    const rate = el('td', { class: 'diary__rate' });
    if (student.attendanceRate === null) {
      rate.append('–');
    } else {
      const fill = el('span', { class: 'rate-bar__fill' });
      fill.style.width = `${student.attendanceRate}%`;
      rate.append(el('span', { class: 'rate-bar', 'aria-hidden': 'true' }, fill), `${student.attendanceRate}%`);
    }
    return el(
      'tr',
      { 'data-left': String(student.enrollmentStatus === 'LEFT') },
      name,
      rate,
      ...diary.dates.map((date) => {
        const mark = byDate.get(date);
        if (!mark) {
          return el('td', { class: 'diary__cell' }, el('span', { class: 'diary__none', 'aria-label': 'sem chamada' }, '–'));
        }
        const cell = el(
          'td',
          { class: `diary__cell diary__cell--${mark.present ? 'present' : 'absent'}` },
          el('abbr', { title: mark.present ? 'Presente' : 'Ausente' }, mark.present ? 'P' : 'F'),
        );
        if (mark.notes) {
          cell.append(el('span', { class: 'diary__note', title: mark.notes }, '*'));
          cell.append(el('span', { class: 'visually-hidden' }, `Observação: ${mark.notes}`));
        }
        return cell;
      }),
    );
  });
  return el(
    'div',
    { class: 'diary-wrap', tabindex: '0', role: 'region', 'aria-label': `Diário de ${diary.title}` },
    el('table', { class: 'diary' }, el('thead', {}, head), el('tbody', {}, ...lines)),
  );
}

function diaryLegend(): HTMLElement {
  return el(
    'p',
    { class: 'diary-legend' },
    'P = presente, F = falta, – = sem chamada, * = tem observação (passe o mouse ou toque para ler). ',
    'A frequência considera todas as chamadas do aluno na turma.',
  );
}

async function studentsTab(body: HTMLElement, activity: Activity): Promise<void> {
  const list = el('div', {}, loading());
  body.replaceChildren(list);
  let enrollments: Enrollment[];
  try {
    enrollments = await api.listEnrollments(activity.id);
  } catch (cause) {
    list.replaceChildren(message('error', errorText(cause)));
    return;
  }
  const reload = () => void studentsTab(body, activity);
  const canManage = context.manages && activity.status === 'ACTIVE';

  const studentRows = enrollments.map((enrollment) => {
    const status =
      enrollment.status === 'ACTIVE'
        ? badge('present', '●', 'Cursando')
        : badge('neutral', '↩', `Saiu em ${formatDate(enrollment.leftAt ?? '')}`);
    const actions: HTMLElement[] = [];
    if (canManage) {
      actions.push(
        enrollment.status === 'ACTIVE'
          ? leaveControl(activity, enrollment, reload, list)
          : button('Voltar a cursar', async () => {
              try {
                await api.changeEnrollment(activity.id, enrollment.beneficiaryId, 'ACTIVE');
                reload();
              } catch (cause) {
                list.prepend(message('error', errorText(cause)));
              }
            }, { small: true }),
      );
    }
    return row(
      [
        el('a', { class: 'row__title', href: `#/beneficiarios/${enrollment.beneficiaryId}` }, enrollment.fullName),
        status,
      ],
      actions,
      avatar(enrollment.fullName),
    );
  });

  const active = enrollments.filter((enrollment) => enrollment.status === 'ACTIVE').length;
  list.replaceChildren(
    el('p', { class: 'row__meta' }, `${active} cursando, ${enrollments.length - active} que saíram.`),
    enrollments.length === 0 ? empty('Nenhum aluno matriculado ainda.') : rows(...studentRows),
  );
  if (canManage) {
    const enrolled = new Set(
      enrollments.filter((enrollment) => enrollment.status === 'ACTIVE').map((enrollment) => enrollment.beneficiaryId),
    );
    list.append(
      section(
        'Matricular aluno',
        el('p', { class: 'row__meta' }, 'Busque entre os beneficiários cadastrados. Quem saiu da turma pode voltar.'),
        beneficiaryPicker(
          'Matricular',
          async (beneficiary) => {
            await api.enroll(activity.id, beneficiary.id);
            enrolled.add(beneficiary.id);
            reload();
          },
          (beneficiaryId) => enrolled.has(beneficiaryId),
        ),
      ),
    );
  }
}

// Leaving needs a date, so the button opens a small inline form instead of acting right away.
function leaveControl(activity: Activity, enrollment: Enrollment, reload: () => void, list: HTMLElement): HTMLElement {
  const wrap = el('div', { class: 'row__actions' });
  const open = button('Registrar saída', () => {
    const date = input({ type: 'date', value: today(), max: today(), 'aria-label': `Data de saída de ${enrollment.fullName}` });
    date.classList.add('input', 'input--inline');
    const confirm = button('Confirmar saída', async () => {
      try {
        await api.changeEnrollment(activity.id, enrollment.beneficiaryId, 'LEFT', date.value);
        reload();
      } catch (cause) {
        list.prepend(message('error', errorText(cause)));
      }
    }, { small: true, variant: 'danger' });
    wrap.replaceChildren(date, confirm, button('Cancelar', () => wrap.replaceChildren(open), { small: true, variant: 'ghost' }));
    date.focus();
  }, { small: true });
  wrap.append(open);
  return wrap;
}

function iconButton(name: IconName, label: string, onClick: () => void): HTMLButtonElement {
  const element = el(
    'button',
    { type: 'button', class: 'button button--secondary button--small button--icon', 'aria-label': label, title: label },
    icon(name),
  );
  element.addEventListener('click', onClick);
  return element;
}
