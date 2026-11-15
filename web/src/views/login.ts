import { api, ApiError, session } from '../api.ts';
import { el } from '../dom.ts';

export function renderLogin(root: HTMLElement, onSuccess: () => void): void {
  const email = el('input', { id: 'email', type: 'email', autocomplete: 'username', required: '' });
  const password = el('input', {
    id: 'password',
    type: 'password',
    autocomplete: 'current-password',
    required: '',
  });
  const error = el('p', { class: 'message message--error', role: 'alert' });
  const submit = el('button', { type: 'submit', class: 'button button--primary' }, 'Entrar');

  const form = el(
    'form',
    { class: 'login-form', novalidate: '' },
    el('div', { class: 'field' }, el('label', { for: 'email' }, 'E-mail'), email),
    el('div', { class: 'field' }, el('label', { for: 'password' }, 'Senha'), password),
    error,
    submit,
  );

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    error.textContent = '';
    if (!email.value || !password.value) {
      error.textContent = 'Informe e-mail e senha.';
      return;
    }
    submit.disabled = true;
    submit.textContent = 'Entrando…';
    try {
      session.save(await api.login(email.value.trim(), password.value));
      onSuccess();
    } catch (cause) {
      error.textContent =
        cause instanceof ApiError && cause.status === 401
          ? 'E-mail ou senha inválidos, ou conta bloqueada temporariamente.'
          : 'Não foi possível conectar ao servidor. Tente novamente.';
      submit.disabled = false;
      submit.textContent = 'Entrar';
    }
  });

  root.replaceChildren(
    el(
      'main',
      { class: 'login' },
      el('img', { class: 'login__logo', src: '/logo.jpg', alt: 'Girassol, logotipo do Lar de Margarida' }),
      el('h1', { class: 'login__title' }, 'Zelo'),
      el('p', { class: 'login__subtitle' }, 'Lar de Margarida · acesso da equipe'),
      form,
    ),
  );
  email.focus();
}
