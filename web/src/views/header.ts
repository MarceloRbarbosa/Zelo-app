import { el } from '../dom.ts';

export function appHeader(onLogout: () => void): HTMLElement {
  const logout = el('button', { type: 'button', class: 'button button--ghost' }, 'Sair');
  logout.addEventListener('click', onLogout);
  return el(
    'header',
    { class: 'app-header' },
    el(
      'div',
      { class: 'app-header__brand' },
      el('img', { src: '/logo.jpg', alt: '', width: '40', height: '40' }),
      el('span', {}, el('strong', {}, 'Zelo'), ' · Lar de Margarida'),
    ),
    logout,
  );
}
