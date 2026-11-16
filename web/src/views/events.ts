import { api, type Participant, type StaffMember, type ZeloEvent } from '../api.ts';
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
  row,
  rows,
  section,
  showFieldErrors,
  textarea,
} from '../design-system/ui.ts';
import { el } from '../dom.ts';
import {
  addMonths,
  formatDateTime,
  formatMonth,
  instantToLocalInput,
  localInputToInstant,
  monthRange,
  parseDate,
  today,
} from '../format.ts';
import { navigate } from '../router.ts';
import { beneficiaryPicker } from './picker.ts';

export function eventStatusBadge(event: ZeloEvent): HTMLElement {
  switch (event.status) {
    case 'SCHEDULED':
      return badge('present', '●', 'Agendado');
    case 'COMPLETED':
      return badge('neutral', '✓', 'Concluído');
    default:
      return badge('absent', '✗', 'Cancelado');
  }
}

// Month bounds in the device's time zone, sent as instants.
function monthInstants(month: string): { from: string; to: string } {
  const { from, to } = monthRange(month);
  const end = parseDate(to);
  end.setHours(23, 59, 59, 999);
  return { from: parseDate(from).toISOString(), to: end.toISOString() };
}

export async function renderEvents(main: HTMLElement, month = today()): Promise<void> {
  const create = context.manages ? link('Novo evento', '#/eventos/novo', 'primary') : null;
  const list = el('div', {}, loading());
  main.replaceChildren(
    pageHeader('Eventos', 'Ações pontuais: feiras, palestras, passeios.', ...(create ? [create] : [])),
    el(
      'div',
      { class: 'diary-toolbar' },
      el('h2', { class: 'diary-toolbar__month' }, formatMonth(month)),
      el(
        'div',
        { class: 'row__actions' },
        button('Mês anterior', () => void renderEvents(main, addMonths(month, -1)), { small: true }),
        button('Próximo mês', () => void renderEvents(main, addMonths(month, 1)), { small: true }),
      ),
    ),
    list,
  );
  try {
    const { from, to } = monthInstants(month);
    const events = await api.listEvents(from, to);
    list.replaceChildren(
      events.length === 0
        ? empty('Nenhum evento neste mês.')
        : rows(
            ...events.map((event) =>
              row(
                [
                  el('a', { class: 'row__title', href: `#/eventos/${event.id}` }, event.title),
                  el(
                    'span',
                    { class: 'row__meta' },
                    event.location ? `${formatDateTime(event.eventDate)}, ${event.location}` : formatDateTime(event.eventDate),
                  ),
                  eventStatusBadge(event),
                ],
                [link('Abrir', `#/eventos/${event.id}`)],
              ),
            ),
          ),
    );
  } catch (cause) {
    list.replaceChildren(message('error', errorText(cause)));
  }
}

export async function renderEventForm(main: HTMLElement, id?: number): Promise<void> {
  main.replaceChildren(loading());
  let event: ZeloEvent | null = null;
  let staff: StaffMember[];
  try {
    [event, staff] = await Promise.all([id ? api.getEvent(id) : Promise.resolve(null), api.listStaff()]);
  } catch (cause) {
    main.replaceChildren(message('error', errorText(cause, 'Evento não encontrado.')));
    return;
  }
  const title = field('Nome do evento', input({ maxlength: '150', value: event?.title ?? '' }));
  const eventDate = field(
    'Data e hora',
    input({ type: 'datetime-local', value: event ? instantToLocalInput(event.eventDate) : '' }),
  );
  const location = field('Local', input({ maxlength: '150', value: event?.location ?? '' }), 'Opcional.');
  const description = field('Descrição', textarea(event?.description ?? ''), 'Opcional.');
  const selected = new Set(event?.staff.map((member) => member.id) ?? []);
  const staffChecks = staff
    .filter((member) => member.status === 'ACTIVE' || selected.has(member.id))
    .map((member) => ({ id: member.id, ...checkbox(member.fullName, selected.has(member.id)) }));
  const staffError = el('p', { class: 'field__error' });
  const feedback = el('div', {});
  const save = button(event ? 'Salvar alterações' : 'Criar evento', undefined, { variant: 'primary', type: 'submit' });

  const form = el(
    'form',
    { class: 'form', novalidate: '' },
    title.root,
    el('div', { class: 'form__row' }, eventDate.root, location.root),
    description.root,
    el(
      'fieldset',
      {},
      el('legend', {}, 'Responsáveis'),
      el('p', { class: 'field__hint' }, 'Quem inscreve os beneficiários e marca quem participou.'),
      ...staffChecks.map((check) => check.root),
      staffError,
    ),
    feedback,
    el('div', { class: 'form__actions' }, save, link('Cancelar', event ? `#/eventos/${event.id}` : '#/eventos', 'ghost')),
  );
  form.addEventListener('submit', async (submitEvent) => {
    submitEvent.preventDefault();
    feedback.replaceChildren();
    [title, eventDate].forEach((item) => item.setError(''));
    staffError.textContent = '';
    const staffIds = staffChecks.filter((check) => check.input.checked).map((check) => check.id);
    let valid = true;
    if (!title.control.value.trim()) {
      title.setError('Dê um nome ao evento.');
      valid = false;
    }
    if (!eventDate.control.value) {
      eventDate.setError('Informe data e hora.');
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
      eventDate: localInputToInstant(eventDate.control.value),
      location: location.control.value.trim() || null,
      staffIds,
    };
    try {
      const saved = event ? await api.updateEvent(event.id, body) : await api.createEvent(body);
      navigate(`/eventos/${saved.id}`);
    } catch (cause) {
      if (!showFieldErrors(cause, { title, eventDate, location })) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
      save.disabled = false;
    }
  });
  main.replaceChildren(pageHeader(event ? `Editar ${event.title}` : 'Novo evento'), form);
  title.control.focus();
}

export async function renderEvent(main: HTMLElement, id: number): Promise<void> {
  main.replaceChildren(loading());
  let event: ZeloEvent;
  try {
    event = await api.getEvent(id);
  } catch (cause) {
    main.replaceChildren(message('error', errorText(cause, 'Evento não encontrado.')));
    return;
  }
  const feedback = el('div', {});
  const actions: HTMLElement[] = [];
  if (context.manages && event.status === 'SCHEDULED') {
    const change = (label: string, call: () => Promise<ZeloEvent>, confirmText: string) =>
      button(label, async () => {
        if (!window.confirm(confirmText)) {
          return;
        }
        try {
          await call();
          void renderEvent(main, id);
        } catch (cause) {
          feedback.replaceChildren(message('error', errorText(cause)));
        }
      }, { variant: label === 'Cancelar evento' ? 'danger' : 'secondary' });
    actions.push(
      link('Editar', `#/eventos/${id}/editar`),
      change('Marcar como concluído', () => api.completeEvent(id), 'Marcar o evento como concluído?'),
      change('Cancelar evento', () => api.cancelEvent(id), 'Cancelar o evento? As inscrições ficam bloqueadas.'),
    );
  }
  main.replaceChildren(
    pageHeader(event.title, event.description ?? undefined, ...actions),
    feedback,
    el(
      'div',
      { class: 'activity-details' },
      eventStatusBadge(event),
      el('p', {}, formatDateTime(event.eventDate)),
      ...(event.location ? [el('p', {}, event.location)] : []),
      el('p', { class: 'row__meta' }, `Responsáveis: ${event.staff.map((staff) => staff.fullName).join(', ')}`),
    ),
  );

  const responsible = context.manages || event.staff.some((staff) => staff.id === context.profile.id);
  if (!responsible) {
    main.append(message('info', 'A lista de inscritos fica com os responsáveis pelo evento.'));
    return;
  }
  const participants = el('div', {}, loading());
  main.append(section('Inscritos', participants));
  await participantsSection(participants, event);
}

async function participantsSection(container: HTMLElement, event: ZeloEvent): Promise<void> {
  let list: Participant[];
  try {
    list = await api.listParticipants(event.id);
  } catch (cause) {
    container.replaceChildren(message('error', errorText(cause)));
    return;
  }
  const reload = () => void participantsSection(container, event);
  const open = event.status !== 'CANCELED';
  const attended = list.filter((participant) => participant.attended).length;

  const items = list.map((participant) => {
    const actions: HTMLElement[] = [];
    if (open) {
      const check = checkbox('Participou', participant.attended);
      check.input.addEventListener('change', async () => {
        try {
          await api.markParticipant(event.id, participant.beneficiaryId, check.input.checked);
          reload();
        } catch (cause) {
          container.prepend(message('error', errorText(cause)));
        }
      });
      actions.push(
        check.root,
        button('Remover inscrição', async () => {
          try {
            await api.unregisterParticipant(event.id, participant.beneficiaryId);
            reload();
          } catch (cause) {
            container.prepend(message('error', errorText(cause)));
          }
        }, { small: true, variant: 'ghost' }),
      );
    }
    return row(
      [
        el('span', { class: 'row__title' }, participant.fullName),
        participant.attended ? badge('present', '✓', 'Participou') : badge('neutral', '○', 'Inscrito'),
      ],
      actions,
      avatar(participant.fullName),
    );
  });

  container.replaceChildren(
    el('p', { class: 'row__meta' }, `${list.length} inscritos, ${attended} participaram.`),
    list.length === 0 ? empty('Ninguém inscrito ainda.') : rows(...items),
  );
  if (open) {
    const registered = new Set(list.map((participant) => participant.beneficiaryId));
    container.append(
      section(
        'Inscrever beneficiário',
        beneficiaryPicker(
          'Inscrever',
          async (beneficiary) => {
            await api.registerParticipant(event.id, beneficiary.id);
            registered.add(beneficiary.id);
            reload();
          },
          (beneficiaryId) => registered.has(beneficiaryId),
        ),
      ),
    );
  }
}
