import { context, ROLE_LABEL } from './context.ts';
import { icon, type IconName } from './design-system/icons.ts';
import { avatar } from './design-system/ui.ts';
import { el } from './dom.ts';

interface NavItem {
  href: string;
  label: string;
  icon: IconName;
  visible: () => boolean;
}

// Daily work first; management areas in their own group.
const NAV: NavItem[] = [
  { href: '#/', label: 'Agenda', icon: 'calendar', visible: () => true },
  { href: '#/turmas', label: 'Turmas', icon: 'book', visible: () => true },
  { href: '#/beneficiarios', label: 'Beneficiários', icon: 'people', visible: () => true },
  { href: '#/eventos', label: 'Eventos', icon: 'star', visible: () => true },
  { href: '#/assistencia', label: 'Assistência', icon: 'box', visible: () => context.manages },
];

const MANAGEMENT: NavItem[] = [
  { href: '#/equipe', label: 'Equipe', icon: 'user', visible: () => context.manages },
  { href: '#/organizacao', label: 'Organização', icon: 'shield', visible: () => context.is('ROLE_ADMIN') },
];

const ACCOUNT: NavItem[] = [{ href: '#/perfil', label: 'Meu perfil', icon: 'settings', visible: () => true }];

function navLinks(items: NavItem[]): HTMLElement[] {
  return items
    .filter((item) => item.visible())
    .map((item) => el('li', {}, el('a', { href: item.href, class: 'nav__link' }, icon(item.icon), item.label)));
}

// Side menu on wide screens; on phones it slides in from the menu button in the top bar.
export function renderShell(root: HTMLElement, onLogout: () => void): HTMLElement {
  const main = el('main', { class: 'shell__main', id: 'conteudo', tabindex: '-1' });

  const logout = el('button', { type: 'button', class: 'nav__logout', 'aria-label': 'Sair', title: 'Sair' }, icon('logout'));
  logout.addEventListener('click', onLogout);

  const nav = el(
    'nav',
    { class: 'nav', id: 'menu', 'aria-label': 'Menu principal' },
    el(
      'div',
      { class: 'nav__brand' },
      el('span', { class: 'brand-mark', 'aria-hidden': 'true' }, 'Z'),
      el('div', {}, el('p', { class: 'nav__name' }, 'Zelo'), el('p', { class: 'nav__org' }, 'Lar de Margarida')),
    ),
    el('ul', { class: 'nav__list' }, ...navLinks(NAV)),
    ...(context.manages
      ? [el('p', { class: 'nav__group' }, 'Gestão'), el('ul', { class: 'nav__list' }, ...navLinks(MANAGEMENT))]
      : []),
    el('p', { class: 'nav__group' }, 'Conta'),
    el('ul', { class: 'nav__list' }, ...navLinks(ACCOUNT)),
    el(
      'div',
      { class: 'nav__footer' },
      avatar(context.profile.fullName),
      el(
        'div',
        { class: 'nav__who' },
        el('p', { class: 'nav__user' }, context.profile.fullName),
        el('p', { class: 'nav__role' }, ROLE_LABEL[context.profile.role]),
      ),
      logout,
    ),
  );

  const toggle = el(
    'button',
    { type: 'button', class: 'topbar__menu', 'aria-expanded': 'false', 'aria-controls': 'menu', 'aria-label': 'Abrir menu' },
    icon('menu'),
  );
  const setOpen = (open: boolean) => {
    toggle.setAttribute('aria-expanded', String(open));
    nav.classList.toggle('nav--open', open);
  };
  toggle.addEventListener('click', () => setOpen(toggle.getAttribute('aria-expanded') !== 'true'));
  nav.addEventListener('click', (event) => {
    if ((event.target as HTMLElement).closest('a')) {
      setOpen(false);
    }
  });
  window.addEventListener('hashchange', () => setOpen(false));
  nav.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && nav.classList.contains('nav--open')) {
      setOpen(false);
      toggle.focus();
    }
  });

  const topbar = el(
    'header',
    { class: 'topbar' },
    toggle,
    el('p', { class: 'topbar__name' }, el('span', { class: 'brand-mark', 'aria-hidden': 'true' }, 'Z'), 'Zelo'),
  );

  root.replaceChildren(
    el('a', { href: '#conteudo', class: 'skip-link' }, 'Pular para o conteúdo'),
    el('div', { class: 'shell' }, topbar, nav, main),
  );
  return main;
}

// Marks the menu entry of the current page; sub-pages (#/turmas/3) keep their section marked.
export function markCurrent(path: string): void {
  for (const link of document.querySelectorAll<HTMLAnchorElement>('.nav__link')) {
    const target = link.getAttribute('href')?.slice(1) ?? '';
    const current = target === '/' ? path === '/' : path === target || path.startsWith(`${target}/`);
    if (current) {
      link.setAttribute('aria-current', 'page');
    } else {
      link.removeAttribute('aria-current');
    }
  }
}
