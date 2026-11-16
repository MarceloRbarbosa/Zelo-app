import { context, ROLE_LABEL } from './context.ts';
import { el } from './dom.ts';

interface NavItem {
  href: string;
  label: string;
  visible: () => boolean;
}

const NAV: NavItem[] = [
  { href: '#/', label: 'Início', visible: () => true },
  { href: '#/turmas', label: 'Turmas', visible: () => true },
  { href: '#/beneficiarios', label: 'Beneficiários', visible: () => true },
  { href: '#/eventos', label: 'Eventos', visible: () => true },
  { href: '#/calendario', label: 'Calendário', visible: () => true },
  { href: '#/assistencia', label: 'Assistência', visible: () => context.manages },
  { href: '#/equipe', label: 'Equipe', visible: () => context.is('ROLE_ADMIN') },
  { href: '#/perfil', label: 'Meu perfil', visible: () => true },
];

// Side menu on wide screens; on phones it slides in from the menu button in the top bar.
export function renderShell(root: HTMLElement, onLogout: () => void): HTMLElement {
  const main = el('main', { class: 'shell__main', id: 'conteudo', tabindex: '-1' });

  const links = NAV.filter((item) => item.visible()).map((item) =>
    el('li', {}, el('a', { href: item.href, class: 'nav__link' }, item.label)),
  );

  const logout = el('button', { type: 'button', class: 'nav__logout' }, 'Sair');
  logout.addEventListener('click', onLogout);

  const nav = el(
    'nav',
    { class: 'nav', id: 'menu', 'aria-label': 'Menu principal' },
    el(
      'div',
      { class: 'nav__brand' },
      el('img', { src: '/logo.jpg', alt: '', width: '48', height: '48' }),
      el('div', {}, el('p', { class: 'nav__name' }, 'Zelo'), el('p', { class: 'nav__org' }, 'Lar de Margarida')),
    ),
    el('ul', { class: 'nav__list' }, ...links),
    el(
      'div',
      { class: 'nav__footer' },
      el('p', { class: 'nav__user' }, context.profile.fullName),
      el('p', { class: 'nav__role' }, ROLE_LABEL[context.profile.role]),
      logout,
    ),
  );

  const toggle = el(
    'button',
    { type: 'button', class: 'topbar__menu', 'aria-expanded': 'false', 'aria-controls': 'menu' },
    el('span', { 'aria-hidden': 'true' }, '☰'),
    ' Menu',
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
    el('p', { class: 'topbar__name' }, el('img', { src: '/logo.jpg', alt: '', width: '32', height: '32' }), 'Zelo'),
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
