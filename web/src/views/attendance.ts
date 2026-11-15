import { api, ApiError, type Activity, type Beneficiary } from '../api.ts';
import { el } from '../dom.ts';
import { appHeader } from './header.ts';

type Mark = 'present' | 'absent';

const STATUS_LABEL: Record<Mark | 'unmarked', { icon: string; text: string }> = {
  present: { icon: '✓', text: 'Presente' },
  absent: { icon: '✗', text: 'Ausente' },
  unmarked: { icon: '○', text: 'Não marcado' },
};

function today(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

function errorMessage(cause: unknown): string {
  if (!(cause instanceof ApiError)) {
    return 'Não foi possível conectar ao servidor. Tente novamente.';
  }
  switch (cause.status) {
    case 409:
      return 'A chamada desta oficina já foi registrada nesta data para estes beneficiários.';
    case 400:
      return 'Não foi possível salvar: verifique a data e os beneficiários marcados.';
    case 403:
      return 'Seu perfil não tem permissão para esta ação.';
    case 404:
      return 'Oficina não encontrada. Recarregue a página.';
    default:
      return 'Erro inesperado ao salvar a chamada. Tente novamente.';
  }
}

export async function renderAttendance(root: HTMLElement, onLogout: () => void): Promise<void> {
  const main = el('main', { class: 'page' }, el('p', { role: 'status' }, 'Carregando…'));
  root.replaceChildren(appHeader(onLogout), main);

  let activities: Activity[];
  let beneficiaries: Beneficiary[];
  try {
    [activities, beneficiaries] = await Promise.all([api.listActivities(), api.listBeneficiaries()]);
  } catch (cause) {
    main.replaceChildren(el('p', { class: 'message message--error', role: 'alert' }, errorMessage(cause)));
    return;
  }
  if (activities.length === 0 || beneficiaries.length === 0) {
    main.replaceChildren(
      el('h1', {}, 'Chamada do dia'),
      el('p', { class: 'message' }, 'Cadastre ao menos uma oficina e um beneficiário para fazer a chamada.'),
    );
    return;
  }

  const marks = new Map<number, Mark>();

  const activitySelect = el(
    'select',
    { id: 'activity' },
    ...activities.map((activity) => el('option', { value: String(activity.id) }, activity.title)),
  );
  const dateInput = el('input', { id: 'date', type: 'date', value: today(), max: today() });
  const progress = el('p', { class: 'progress', 'aria-live': 'polite' });
  const feedback = el('div', { class: 'feedback' });
  const markAll = el('button', { type: 'button', class: 'button button--secondary' }, 'Marcar todos como presentes');
  const save = el('button', { type: 'button', class: 'button button--primary' }, 'Salvar chamada');

  const cards = beneficiaries.map((beneficiary) => {
    const status = el('span', { class: 'status' });
    const presentButton = el(
      'button',
      { type: 'button', class: 'toggle toggle--present', 'aria-pressed': 'false' },
      '✓ Presente',
    );
    const absentButton = el(
      'button',
      { type: 'button', class: 'toggle toggle--absent', 'aria-pressed': 'false' },
      '✗ Ausente',
    );
    const card = el(
      'li',
      { class: 'card' },
      el('div', { class: 'card__header' }, el('span', { class: 'card__name' }, beneficiary.fullName), status),
      el(
        'div',
        { class: 'card__actions', role: 'group', 'aria-label': `Presença de ${beneficiary.fullName}` },
        presentButton,
        absentButton,
      ),
    );

    const paint = () => {
      const mark = marks.get(beneficiary.id);
      const label = STATUS_LABEL[mark ?? 'unmarked'];
      card.dataset.state = mark ?? 'unmarked';
      status.replaceChildren(el('span', { 'aria-hidden': 'true' }, label.icon), ` ${label.text}`);
      presentButton.setAttribute('aria-pressed', String(mark === 'present'));
      absentButton.setAttribute('aria-pressed', String(mark === 'absent'));
    };
    const choose = (mark: Mark) => {
      marks.set(beneficiary.id, mark);
      paint();
      update();
    };
    presentButton.addEventListener('click', () => choose('present'));
    absentButton.addEventListener('click', () => choose('absent'));
    paint();
    return { card, paint };
  });

  const update = () => {
    progress.textContent = `${marks.size} de ${beneficiaries.length} marcados`;
    save.disabled = marks.size < beneficiaries.length;
    feedback.replaceChildren();
  };

  markAll.addEventListener('click', () => {
    for (const beneficiary of beneficiaries) {
      marks.set(beneficiary.id, 'present');
    }
    cards.forEach(({ paint }) => paint());
    update();
  });
  activitySelect.addEventListener('change', update);
  dateInput.addEventListener('change', update);

  save.addEventListener('click', async () => {
    save.disabled = true;
    save.textContent = 'Salvando…';
    try {
      const result = await api.registerAttendance({
        activityId: Number(activitySelect.value),
        attendanceDate: dateInput.value,
        entries: beneficiaries.map((beneficiary) => ({
          beneficiaryId: beneficiary.id,
          present: marks.get(beneficiary.id) === 'present',
        })),
      });
      feedback.replaceChildren(
        el(
          'p',
          { class: 'message message--success', role: 'status' },
          `✓ Chamada salva: ${result.present} presentes e ${result.absent} ausentes.`,
        ),
      );
    } catch (cause) {
      feedback.replaceChildren(
        el('p', { class: 'message message--error', role: 'alert' }, `✗ ${errorMessage(cause)}`),
      );
      save.disabled = false;
    } finally {
      save.textContent = 'Salvar chamada';
    }
  });

  main.replaceChildren(
    el('h1', {}, 'Chamada do dia'),
    el(
      'div',
      { class: 'filters' },
      el('div', { class: 'field' }, el('label', { for: 'activity' }, 'Oficina'), activitySelect),
      el('div', { class: 'field' }, el('label', { for: 'date' }, 'Data'), dateInput),
    ),
    el('div', { class: 'toolbar' }, progress, markAll),
    el('ul', { class: 'cards', 'aria-label': 'Beneficiários' }, ...cards.map(({ card }) => card)),
    el('div', { class: 'footer-bar' }, feedback, save),
  );
  update();
}
