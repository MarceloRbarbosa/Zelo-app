import { api, type Activity, type Beneficiary, type BeneficiaryRequest, type Delivery } from '../api.ts';
import { context } from '../context.ts';
import {

  button,
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
  select,
  showFieldErrors,
  textarea,
} from '../design-system/ui.ts';
import { el } from '../dom.ts';
import { formatDate, today } from '../format.ts';
import { navigate } from '../router.ts';

export async function renderBeneficiaries(main: HTMLElement): Promise<void> {
  const name = field('Nome', input({ type: 'search', autocomplete: 'off' }));
  const document = field('Documento (CPF)', input({ inputmode: 'numeric', maxlength: '11' }), '11 dígitos, sem pontos.');
  const activity = field('Turma', select([{ value: '', label: 'Todas' }]));
  const order = field(
    'Ordem',
    select([
      { value: 'ASC', label: 'Nome de A a Z' },
      { value: 'DESC', label: 'Nome de Z a A' },
    ]),
  );
  const results = el('div', { 'aria-live': 'polite' }, loading());
  const form = el(
    'form',
    { class: 'form search-form', role: 'search' },
    el('div', { class: 'form__row' }, name.root, document.root),
    el('div', { class: 'form__row' }, activity.root, order.root),
    el('div', { class: 'form__actions' }, button('Pesquisar', undefined, { type: 'submit', variant: 'primary' })),
  );
  const create = context.manages ? link('Cadastrar beneficiário', '#/beneficiarios/novo', 'primary') : null;
  main.replaceChildren(
    pageHeader('Beneficiários', 'Crianças e jovens atendidos pela ONG.', ...(create ? [create] : [])),
    form,
    section('Resultado', results),
  );

  api
    .listActivities()
    .then((activities: Activity[]) =>
      activities.forEach((item) => activity.control.append(el('option', { value: String(item.id) }, item.title))),
    )
    .catch(() => undefined);

  const search = async () => {
    results.replaceChildren(loading());
    try {
      const found = await api.searchBeneficiaries({
        name: name.control.value.trim() || undefined,
        documentNumber: document.control.value.trim() || undefined,
        activityId: activity.control.value ? Number(activity.control.value) : undefined,
        order: order.control.value as 'ASC' | 'DESC',
      });
      results.replaceChildren(
        found.length === 0
          ? empty('Ninguém encontrado com esses filtros.')
          : el(
              'div',
              { class: 'stack' },
              el('p', { class: 'row__meta' }, `${found.length} ${found.length === 1 ? 'pessoa' : 'pessoas'}`),
              rows(
                ...found.map((beneficiary) =>
                  row(
                    [
                      el('a', { class: 'row__title', href: `#/beneficiarios/${beneficiary.id}` }, beneficiary.fullName),
                      el(
                        'span',
                        { class: 'row__meta' },
                        `Nascimento ${formatDate(beneficiary.birthDate)}, telefone ${beneficiary.phoneNumber}`,
                      ),
                    ],
                    [link('Ver ficha', `#/beneficiarios/${beneficiary.id}`)],
                  ),
                ),
              ),
            ),
      );
    } catch (cause) {
      results.replaceChildren(message('error', errorText(cause)));
    }
  };
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    void search();
  });
  await search();
}

export async function renderBeneficiary(main: HTMLElement, id: number): Promise<void> {
  main.replaceChildren(loading());
  let beneficiary: Beneficiary;
  try {
    beneficiary = await api.getBeneficiary(id);
  } catch (cause) {
    main.replaceChildren(message('error', errorText(cause, 'Beneficiário não encontrado ou inativo.')));
    return;
  }
  const actions: HTMLElement[] = [];
  const feedback = el('div', {});
  if (context.manages) {
    actions.push(
      link('Editar', `#/beneficiarios/${id}/editar`),
      button('Inativar', async () => {
        if (!window.confirm(`Inativar ${beneficiary.fullName}? A ficha some das pesquisas, das turmas e dos alertas.`)) {
          return;
        }
        try {
          await api.deactivateBeneficiary(id);
          navigate('/beneficiarios');
        } catch (cause) {
          feedback.replaceChildren(message('error', errorText(cause)));
        }
      }, { variant: 'danger' }),
    );
  }
  const facts = el(
    'dl',
    { class: 'facts' },
    ...fact('Nascimento', formatDate(beneficiary.birthDate)),
    ...fact('Telefone', el('a', { href: `tel:${beneficiary.phoneNumber}` }, beneficiary.phoneNumber)),
    ...fact('Documento', beneficiary.documentNumber ?? 'Não informado'),
    ...fact('Endereço', beneficiary.address ?? 'Não informado'),
  );
  main.replaceChildren(pageHeader(beneficiary.fullName, undefined, ...actions), feedback, facts);

  if (context.manages) {
    const deliveries = el('div', {}, loading());
    main.append(section('Assistência recebida', deliveries));
    try {
      const list: Delivery[] = await api.listBeneficiaryDeliveries(id);
      deliveries.replaceChildren(
        list.length === 0
          ? empty('Nenhuma entrega registrada para esta pessoa.')
          : rows(
              ...list.map((delivery) =>
                row([
                  el('span', { class: 'row__title' }, `${delivery.quantity} × ${delivery.itemName}`),
                  el('span', { class: 'row__meta' }, formatDate(delivery.deliveredAt)),
                ]),
              ),
            ),
      );
    } catch (cause) {
      deliveries.replaceChildren(message('error', errorText(cause)));
    }
  }
}

function fact(term: string, value: string | Node): HTMLElement[] {
  return [el('dt', {}, term), el('dd', {}, value)];
}

export async function renderBeneficiaryForm(main: HTMLElement, id?: number): Promise<void> {
  let beneficiary: Beneficiary | null = null;
  if (id) {
    main.replaceChildren(loading());
    try {
      beneficiary = await api.getBeneficiary(id);
    } catch (cause) {
      main.replaceChildren(message('error', errorText(cause, 'Beneficiário não encontrado ou inativo.')));
      return;
    }
  }
  const fullName = field('Nome completo', input({ maxlength: '150', value: beneficiary?.fullName ?? '' }));
  const birthDate = field('Data de nascimento', input({ type: 'date', max: today(), value: beneficiary?.birthDate ?? '' }));
  const phoneNumber = field(
    'Telefone para contato',
    input({ type: 'tel', maxlength: '20', value: beneficiary?.phoneNumber ?? '' }),
    'Usado para a busca ativa quando a criança falta.',
  );
  const documentNumber = field(
    'Documento (CPF)',
    input({ inputmode: 'numeric', maxlength: '11', value: beneficiary?.documentNumber ?? '' }),
    'Opcional. 11 dígitos, sem pontos.',
  );
  const address = field('Endereço', textarea(beneficiary?.address ?? ''), 'Opcional.');
  const feedback = el('div', {});
  const save = button(beneficiary ? 'Salvar alterações' : 'Cadastrar', undefined, { variant: 'primary', type: 'submit' });
  const form = el(
    'form',
    { class: 'form', novalidate: '' },
    fullName.root,
    el('div', { class: 'form__row' }, birthDate.root, phoneNumber.root),
    documentNumber.root,
    address.root,
    feedback,
    el(
      'div',
      { class: 'form__actions' },
      save,
      link('Cancelar', beneficiary ? `#/beneficiarios/${beneficiary.id}` : '#/beneficiarios', 'ghost'),
    ),
  );
  const fields = { fullName, birthDate, phoneNumber, documentNumber, address };
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    Object.values(fields).forEach((item) => item.setError(''));
    const body: BeneficiaryRequest = {
      fullName: fullName.control.value.trim(),
      birthDate: birthDate.control.value,
      phoneNumber: phoneNumber.control.value.trim(),
      documentNumber: documentNumber.control.value.trim() || null,
      address: address.control.value.trim() || null,
    };
    save.disabled = true;
    try {
      const saved = beneficiary
        ? await api.updateBeneficiary(beneficiary.id, body)
        : await api.createBeneficiary(body);
      navigate(`/beneficiarios/${saved.id}`);
    } catch (cause) {
      if (!showFieldErrors(cause, fields)) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
      save.disabled = false;
    }
  });
  main.replaceChildren(
    pageHeader(beneficiary ? `Editar ${beneficiary.fullName}` : 'Cadastrar beneficiário'),
    form,
  );
  fullName.control.focus();
}

