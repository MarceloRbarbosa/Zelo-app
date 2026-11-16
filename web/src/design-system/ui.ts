// Design system builders. Text always goes in as text nodes (see el), never as HTML.
import { ApiError } from '../api.ts';
import { el } from '../dom.ts';
import { icon, type IconName } from './icons.ts';

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger' | 'danger-solid' | 'sun';

export function button(
  label: string,
  onClick?: (event: MouseEvent) => void,
  options: { variant?: ButtonVariant; type?: 'button' | 'submit'; small?: boolean; icon?: IconName } = {},
): HTMLButtonElement {
  const variant = options.variant ?? 'secondary';
  const classes = ['button', `button--${variant}`];
  if (options.small) {
    classes.push('button--small');
  }
  const element = el('button', { type: options.type ?? 'button', class: classes.join(' ') });
  if (options.icon) {
    element.append(icon(options.icon));
  }
  element.append(label);
  if (onClick) {
    element.addEventListener('click', onClick);
  }
  return element;
}

export function link(
  label: string,
  href: string,
  variant: ButtonVariant = 'secondary',
  options: { small?: boolean; icon?: IconName } = {},
): HTMLAnchorElement {
  const element = el('a', { href, class: `button button--${variant}${options.small ? ' button--small' : ''}` });
  if (options.icon) {
    element.append(icon(options.icon));
  }
  element.append(label);
  return element;
}

let fieldCount = 0;

export interface Field<T extends HTMLElement> {
  root: HTMLElement;
  control: T;
  setError(message: string): void;
}

// Label, control, optional hint and an error line tied to the control for screen readers.
export function field<T extends HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>(
  label: string,
  control: T,
  hint?: string,
): Field<T> {
  fieldCount += 1;
  const id = control.id || `field-${fieldCount}`;
  control.id = id;
  control.classList.add('input');
  const error = el('p', { class: 'field__error', id: `${id}-error` });
  const described = [`${id}-error`];
  // Helper text sits under the control, so fields side by side keep their inputs aligned.
  const children: Node[] = [el('label', { class: 'field__label', for: id }, label), control];
  if (hint) {
    children.push(el('p', { class: 'field__hint', id: `${id}-hint` }, hint));
    described.push(`${id}-hint`);
  }
  control.setAttribute('aria-describedby', described.join(' '));
  children.push(error);
  return {
    root: el('div', { class: 'field' }, ...children),
    control,
    setError(message: string) {
      error.textContent = message;
      if (message) {
        control.setAttribute('aria-invalid', 'true');
      } else {
        control.removeAttribute('aria-invalid');
      }
    },
  };
}

export function input(attributes: Record<string, string> = {}): HTMLInputElement {
  return el('input', { type: 'text', ...attributes });
}

export function select(options: { value: string; label: string }[], value?: string): HTMLSelectElement {
  const element = el('select', {}, ...options.map((option) => el('option', { value: option.value }, option.label)));
  if (value !== undefined) {
    element.value = value;
  }
  return element;
}

export function textarea(value = ''): HTMLTextAreaElement {
  const element = el('textarea', {});
  element.value = value;
  return element;
}

export function checkbox(label: string, checked = false): { root: HTMLLabelElement; input: HTMLInputElement } {
  const box = el('input', { type: 'checkbox' });
  box.checked = checked;
  return { root: el('label', { class: 'check' }, box, label), input: box };
}

export type BadgeKind = 'present' | 'absent' | 'risk' | 'neutral';

// Screens pass a symbol; the badge draws the matching line icon.
const BADGE_ICONS: Record<string, IconName> = {
  '✓': 'check',
  '✗': 'x',
  '⚠': 'alert',
  '!': 'alert',
  '●': 'dot',
  '■': 'square',
  '○': 'circle',
  '↩': 'undo',
};

// Icon and text always travel together, so the state never depends on color alone.
export function badge(kind: BadgeKind, symbol: string, text: string): HTMLElement {
  return el('span', { class: `badge badge--${kind}` }, icon(BADGE_ICONS[symbol] ?? 'dot'), text);
}

export function riskBadge(): HTMLElement {
  return badge('risk', '⚠', 'Risco de evasão');
}

export function message(kind: 'error' | 'success' | 'info', text: string): HTMLElement {
  const name: IconName = kind === 'error' ? 'alert' : kind === 'success' ? 'check' : 'circle';
  return el(
    'p',
    { class: `message message--${kind}`, role: kind === 'error' ? 'alert' : 'status' },
    icon(name),
    el('span', {}, text),
  );
}

// Initials of the first and last names, as a quick visual anchor next to a person's name.
export function avatar(fullName: string): HTMLElement {
  const parts = fullName.trim().split(/\s+/);
  const initials = (parts[0]?.[0] ?? '') + (parts.length > 1 ? parts[parts.length - 1][0] : '');
  return el('span', { class: 'avatar', 'aria-hidden': 'true' }, initials.toUpperCase());
}

export function pageHeader(title: string, subtitle?: string, ...actions: HTMLElement[]): HTMLElement {
  const text = el('div', { class: 'page-header__text' }, el('h1', {}, title));
  if (subtitle) {
    text.append(el('p', { class: 'page-header__subtitle' }, subtitle));
  }
  return el('header', { class: 'page-header' }, text, el('div', { class: 'page-header__actions' }, ...actions));
}

export function section(title: string, ...children: Node[]): HTMLElement {
  return el('section', { class: 'section' }, el('div', { class: 'section__header' }, el('h2', {}, title)), ...children);
}

export function empty(text: string, action?: HTMLElement): HTMLElement {
  const box = el('div', { class: 'empty' }, el('p', {}, text));
  if (action) {
    box.append(action);
  }
  return box;
}

export function loading(): HTMLElement {
  return el('p', { role: 'status', class: 'row__meta' }, 'Carregando…');
}

export function rows(...items: HTMLElement[]): HTMLUListElement {
  return el('ul', { class: 'rows' }, ...items);
}

// A list row: optional leading element (an avatar), main text and actions on the right.
export function row(main: Node[], actions: Node[] = [], leading?: Node): HTMLLIElement {
  return el(
    'li',
    { class: 'row' },
    ...(leading ? [leading] : []),
    el('div', { class: 'row__main' }, ...main),
    el('div', { class: 'row__actions' }, ...actions),
  );
}

export function tabs(
  items: { id: string; label: string }[],
  active: string,
  onChange: (id: string) => void,
): HTMLElement {
  const list = el('div', { class: 'tabs', role: 'tablist' });
  for (const item of items) {
    const tab = el(
      'button',
      { type: 'button', class: 'tab', role: 'tab', 'aria-selected': String(item.id === active) },
      item.label,
    );
    tab.addEventListener('click', () => onChange(item.id));
    list.append(tab);
  }
  return list;
}

// What went wrong, in terms the team can act on.
export function errorText(cause: unknown, notFound = 'Registro não encontrado.'): string {
  if (!(cause instanceof ApiError)) {
    return 'Sem conexão com o servidor. Confira a internet e tente de novo.';
  }
  switch (cause.status) {
    case 400:
      return Object.keys(cause.errors).length > 0
        ? 'Confira os campos destacados.'
        : `Não foi possível salvar: ${translate(cause.message)}`;
    case 403:
      return 'Seu perfil não tem acesso a esta ação.';
    case 404:
      return notFound;
    case 409:
      return translate(cause.message);
    default:
      return 'Erro inesperado no servidor. Tente de novo em instantes.';
  }
}

// The API answers in English; the most common messages get a pt-BR version for the team.
const TRANSLATIONS: [RegExp, string][] = [
  [/already enrolled/, 'este beneficiário já está matriculado.'],
  [/already registered on/, 'a chamada desta data já foi salva. Use "Corrigir chamada".'],
  [/already registered/, 'este beneficiário já está inscrito.'],
  [/is finished/, 'a turma está finalizada.'],
  [/is canceled/, 'o evento foi cancelado.'],
  [/is completed/, 'o evento já foi concluído.'],
  [/is inactive/, 'o item está inativo.'],
  [/already exists/, 'já existe um item com este nome.'],
  [/Document number already registered/, 'documento já cadastrado.'],
  [/E-mail already registered/, 'e-mail já cadastrado.'],
  [/not enrolled/, 'há beneficiários sem matrícula ativa na turma.'],
  [/End date cannot be before/, 'a data de fim não pode ser antes do início.'],
  [/Schedule end time must be after/, 'o horário de fim deve ser depois do início.'],
  [/Schedule repeated/, 'há horários repetidos.'],
  [/Leaving date is required/, 'informe a data de saída.'],
  [/Current password is incorrect/, 'a senha atual está incorreta.'],
  [/must be different/, 'a nova senha deve ser diferente da atual.'],
  [/cannot block your own account/, 'você não pode bloquear a própria conta.'],
];

function translate(text: string): string {
  const match = TRANSLATIONS.find(([pattern]) => pattern.test(text));
  return match ? match[1] : 'confira os dados e tente de novo.';
}

// Shows field errors from a 400 response on the matching fields; returns whether any was shown.
export function showFieldErrors(cause: unknown, fields: Record<string, { setError(text: string): void }>): boolean {
  if (!(cause instanceof ApiError)) {
    return false;
  }
  let shown = false;
  for (const [name, text] of Object.entries(cause.errors)) {
    const target = fields[name];
    if (target) {
      target.setError(fieldMessage(text));
      shown = true;
    }
  }
  return shown;
}

function fieldMessage(text: string): string {
  if (/must not be (blank|null|empty)/.test(text)) {
    return 'Preencha este campo.';
  }
  if (/size must be between/.test(text)) {
    return 'Tamanho fora do permitido.';
  }
  if (/11 digits/.test(text)) {
    return 'Use 11 dígitos, sem pontos.';
  }
  if (/past/.test(text)) {
    return 'Use uma data que já passou.';
  }
  if (/email/i.test(text)) {
    return 'E-mail inválido.';
  }
  return 'Valor inválido.';
}

export interface DialogOptions {
  title: string;
  icon?: IconName;
  tone?: 'default' | 'warning' | 'danger';
  body: (Node | string)[];
  actions: (close: () => void) => HTMLElement[];
}

// App modal built on <dialog>: the browser keeps focus inside, closes on Esc and dims the page behind it.
export function openDialog(options: DialogOptions): { close: () => void; element: HTMLDialogElement } {
  fieldCount += 1;
  const titleId = `dialog-title-${fieldCount}`;
  const dialog = el('dialog', { class: `dialog dialog--${options.tone ?? 'default'}`, 'aria-labelledby': titleId });
  const close = () => dialog.close();
  // A dialog belongs to the page that opened it: navigating away closes it.
  window.addEventListener('hashchange', close, { once: true });
  dialog.addEventListener('close', () => {
    window.removeEventListener('hashchange', close);
    dialog.remove();
  });
  if (options.icon) {
    dialog.append(el('span', { class: 'dialog__icon' }, icon(options.icon)));
  }
  dialog.append(
    el('h2', { class: 'dialog__title', id: titleId }, options.title),
    el('div', { class: 'dialog__body' }, ...options.body),
    el('div', { class: 'dialog__actions' }, ...options.actions(close)),
  );
  document.body.append(dialog);
  dialog.showModal();
  return { close, element: dialog };
}

// Replaces window.confirm: resolves true only when the person picks the confirming action.
export function confirmDialog(options: {
  title: string;
  message: string;
  confirmLabel: string;
  tone?: 'default' | 'warning' | 'danger';
  icon?: IconName;
}): Promise<boolean> {
  return new Promise((resolve) => {
    let confirmed = false;
    const { element } = openDialog({
      title: options.title,
      icon: options.icon ?? (options.tone === 'danger' ? 'alert' : undefined),
      tone: options.tone,
      body: [el('p', {}, options.message)],
      actions: (close) => [
        button('Cancelar', close),
        button(options.confirmLabel, () => {
          confirmed = true;
          close();
        }, { variant: options.tone === 'danger' ? 'danger-solid' : 'primary' }),
      ],
    });
    element.addEventListener('close', () => resolve(confirmed));
  });
}
