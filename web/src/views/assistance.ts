import { api, type AssistanceItem, type Beneficiary, type Delivery, type Receipt } from '../api.ts';
import {
  badge,
  button,
  empty,
  errorText,
  field,
  input,
  loading,
  message,
  pageHeader,
  section,
  select,
  showFieldErrors,
  tabs,
  textarea,
} from '../design-system/ui.ts';
import { el } from '../dom.ts';
import { addMonths, formatDate, formatMonth, monthRange, today } from '../format.ts';
import { beneficiaryPicker } from './picker.ts';

type Tab = 'itens' | 'recebimentos' | 'entregas';

// Restricted area (admin and coordinator): what the ONG receives and what each beneficiary gets.
export async function renderAssistance(main: HTMLElement, tab: Tab = 'itens'): Promise<void> {
  const body = el('div', {}, loading());
  main.replaceChildren(
    pageHeader('Assistência', 'Itens como cesta básica: o que chega à ONG e o que é entregue a cada beneficiário.'),
    tabs(
      [
        { id: 'itens', label: 'Itens e saldo' },
        { id: 'recebimentos', label: 'Recebimentos' },
        { id: 'entregas', label: 'Entregas' },
      ],
      tab,
      (next) => void renderAssistance(main, next as Tab),
    ),
    body,
  );
  let items: AssistanceItem[];
  try {
    items = await api.listItems();
  } catch (cause) {
    body.replaceChildren(message('error', errorText(cause)));
    return;
  }
  const reload = () => void renderAssistance(main, tab);
  if (tab === 'itens') {
    itemsTab(body, items, reload);
  } else if (tab === 'recebimentos') {
    await receiptsTab(body, items, today());
  } else {
    await deliveriesTab(body, items, today());
  }
}

function itemsTab(body: HTMLElement, items: AssistanceItem[], reload: () => void): void {
  const table =
    items.length === 0
      ? empty('Nenhum item cadastrado. Comece pela cesta básica, por exemplo.')
      : el(
          'div',
          { class: 'table-wrap' },
          el(
            'table',
            { class: 'table' },
            el(
              'thead',
              {},
              el(
                'tr',
                {},
                el('th', { scope: 'col' }, 'Item'),
                el('th', { scope: 'col', class: 'number' }, 'Recebido'),
                el('th', { scope: 'col', class: 'number' }, 'Entregue'),
                el('th', { scope: 'col', class: 'number' }, 'Saldo'),
                el('th', { scope: 'col' }, 'Situação'),
              ),
            ),
            el(
              'tbody',
              {},
              ...items.map((item) => {
                const toggle = button(item.active ? 'Inativar' : 'Reativar', async () => {
                  try {
                    await api.updateItem(item.id, item.name, item.description, !item.active);
                    reload();
                  } catch (cause) {
                    body.prepend(message('error', errorText(cause)));
                  }
                }, { small: true, variant: 'ghost' });
                return el(
                  'tr',
                  {},
                  el('th', { scope: 'row' }, item.name),
                  el('td', { class: 'number' }, String(item.received)),
                  el('td', { class: 'number' }, String(item.delivered)),
                  el(
                    'td',
                    { class: 'number' },
                    item.balance < 0 ? badge('absent', '!', String(item.balance)) : String(item.balance),
                  ),
                  el(
                    'td',
                    {},
                    item.active ? badge('present', '●', 'Ativo') : badge('neutral', '■', 'Inativo'),
                    ' ',
                    toggle,
                  ),
                );
              }),
            ),
          ),
        );

  const name = field('Nome do item', input({ maxlength: '100' }));
  const description = field('Descrição', input({ maxlength: '255' }), 'Opcional. Ex.: "arroz, feijão, óleo, açúcar".');
  const feedback = el('div', {});
  const form = el(
    'form',
    { class: 'form', novalidate: '' },
    el('div', { class: 'form__row' }, name.root, description.root),
    feedback,
    el('div', { class: 'form__actions' }, button('Cadastrar item', undefined, { variant: 'primary', type: 'submit' })),
  );
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    name.setError('');
    if (!name.control.value.trim()) {
      name.setError('Dê um nome ao item.');
      return;
    }
    try {
      await api.createItem(name.control.value.trim(), description.control.value.trim() || null);
      reload();
    } catch (cause) {
      if (!showFieldErrors(cause, { name, description })) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
    }
  });
  body.replaceChildren(
    el('p', { class: 'row__meta' }, 'Saldo = tudo que foi recebido menos tudo que foi entregue.'),
    table,
    section('Novo item', form),
  );
}

function itemSelect(items: AssistanceItem[]): HTMLSelectElement {
  return select(items.filter((item) => item.active).map((item) => ({ value: String(item.id), label: item.name })));
}

function monthNav(month: string, go: (month: string) => void): HTMLElement {
  return el(
    'div',
    { class: 'diary-toolbar' },
    el('h3', { class: 'diary-toolbar__month' }, formatMonth(month)),
    el(
      'div',
      { class: 'row__actions' },
      button('Mês anterior', () => go(addMonths(month, -1)), { small: true }),
      button('Próximo mês', () => go(addMonths(month, 1)), { small: true }),
    ),
  );
}

async function receiptsTab(body: HTMLElement, items: AssistanceItem[], month: string): Promise<void> {
  if (!items.some((item) => item.active)) {
    body.replaceChildren(empty('Cadastre um item na aba "Itens e saldo" antes de registrar recebimentos.'));
    return;
  }
  const item = field('Item', itemSelect(items));
  const quantity = field('Quantidade', input({ type: 'number', min: '1', value: '1' }));
  const receivedAt = field('Recebido em', input({ type: 'date', value: today(), max: today() }));
  const donor = field('Doador', input({ maxlength: '150' }), 'Opcional.');
  const notes = field('Observação', textarea(), 'Opcional.');
  const feedback = el('div', {});
  const form = el(
    'form',
    { class: 'form', novalidate: '' },
    el('div', { class: 'form__row' }, item.root, quantity.root, receivedAt.root),
    donor.root,
    notes.root,
    feedback,
    el('div', { class: 'form__actions' }, button('Registrar recebimento', undefined, { variant: 'primary', type: 'submit' })),
  );
  const list = el('div', {}, loading());
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    [quantity, receivedAt].forEach((target) => target.setError(''));
    try {
      await api.createReceipt({
        itemId: Number(item.control.value),
        quantity: Number(quantity.control.value),
        receivedAt: receivedAt.control.value,
        donor: donor.control.value.trim() || null,
        notes: notes.control.value.trim() || null,
      });
      form.reset();
      receivedAt.control.value = today();
      feedback.replaceChildren(message('success', 'Recebimento registrado.'));
      void loadReceipts(list, month, body, items);
    } catch (cause) {
      if (!showFieldErrors(cause, { quantity, receivedAt, donor, notes })) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
    }
  });
  body.replaceChildren(section('Registrar recebimento', form), section('Recebidos', list));
  await loadReceipts(list, month, body, items);
}

async function loadReceipts(list: HTMLElement, month: string, body: HTMLElement, items: AssistanceItem[]) {
  const { from, to } = monthRange(month);
  list.replaceChildren(monthNav(month, (next) => void loadReceipts(list, next, body, items)), loading());
  try {
    const receipts: Receipt[] = await api.listReceipts(from, to);
    list.lastChild?.remove();
    list.append(
      receipts.length === 0
        ? empty('Nada recebido neste mês.')
        : movementTable(
            ['Data', 'Item', 'Quantidade', 'Doador'],
            receipts.map((receipt) => [
              formatDate(receipt.receivedAt),
              receipt.itemName,
              String(receipt.quantity),
              receipt.donor ?? '—',
            ]),
          ),
    );
  } catch (cause) {
    list.lastChild?.remove();
    list.append(message('error', errorText(cause)));
  }
}

async function deliveriesTab(body: HTMLElement, items: AssistanceItem[], month: string): Promise<void> {
  if (!items.some((item) => item.active)) {
    body.replaceChildren(empty('Cadastre um item na aba "Itens e saldo" antes de registrar entregas.'));
    return;
  }
  let chosen: Beneficiary | null = null;
  const who = el('p', { class: 'chosen', 'aria-live': 'polite' }, 'Nenhum beneficiário escolhido.');
  const item = field('Item', itemSelect(items));
  const quantity = field('Quantidade', input({ type: 'number', min: '1', value: '1' }));
  const deliveredAt = field('Entregue em', input({ type: 'date', value: today(), max: today() }));
  const notes = field('Observação', textarea(), 'Opcional.');
  const feedback = el('div', {});
  const list = el('div', {}, loading());
  const form = el(
    'form',
    { class: 'form', novalidate: '' },
    el('div', { class: 'form__row' }, item.root, quantity.root, deliveredAt.root),
    notes.root,
    feedback,
    el('div', { class: 'form__actions' }, button('Registrar entrega', undefined, { variant: 'primary', type: 'submit' })),
  );
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    if (!chosen) {
      feedback.replaceChildren(message('error', 'Escolha o beneficiário que recebeu.'));
      return;
    }
    try {
      await api.createDelivery({
        itemId: Number(item.control.value),
        beneficiaryId: chosen.id,
        quantity: Number(quantity.control.value),
        deliveredAt: deliveredAt.control.value,
        notes: notes.control.value.trim() || null,
      });
      feedback.replaceChildren(message('success', `Entrega para ${chosen.fullName} registrada.`));
      void loadDeliveries(list, month);
    } catch (cause) {
      if (!showFieldErrors(cause, { quantity, deliveredAt, notes })) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
    }
  });
  body.replaceChildren(
    section(
      'Registrar entrega',
      el('p', { class: 'field__label' }, 'Para quem'),
      who,
      beneficiaryPicker('Escolher', async (beneficiary) => {
        chosen = beneficiary;
        who.replaceChildren(el('strong', {}, beneficiary.fullName));
      }),
      form,
    ),
    section('Entregues', list),
  );
  await loadDeliveries(list, month);
}

async function loadDeliveries(list: HTMLElement, month: string): Promise<void> {
  const { from, to } = monthRange(month);
  list.replaceChildren(monthNav(month, (next) => void loadDeliveries(list, next)), loading());
  try {
    const deliveries: Delivery[] = await api.listDeliveries(from, to);
    list.lastChild?.remove();
    list.append(
      deliveries.length === 0
        ? empty('Nada entregue neste mês.')
        : movementTable(
            ['Data', 'Beneficiário', 'Item', 'Quantidade'],
            deliveries.map((delivery) => [
              formatDate(delivery.deliveredAt),
              delivery.beneficiaryName ?? 'Beneficiário inativo',
              delivery.itemName,
              String(delivery.quantity),
            ]),
          ),
    );
  } catch (cause) {
    list.lastChild?.remove();
    list.append(message('error', errorText(cause)));
  }
}

function movementTable(headers: string[], lines: string[][]): HTMLElement {
  return el(
    'div',
    { class: 'table-wrap' },
    el(
      'table',
      { class: 'table' },
      el('thead', {}, el('tr', {}, ...headers.map((header) => el('th', { scope: 'col' }, header)))),
      el('tbody', {}, ...lines.map((cells) => el('tr', {}, ...cells.map((cell) => el('td', {}, cell))))),
    ),
  );
}
