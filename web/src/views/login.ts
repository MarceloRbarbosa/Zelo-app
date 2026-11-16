import { api, ApiError, session } from '../api.ts';
import { button, field, input, message } from '../design-system/ui.ts';
import { icon } from '../design-system/icons.ts';
import { el } from '../dom.ts';

export function renderLogin(root: HTMLElement, onSuccess: () => void): void {
  const email = field('E-mail', input({ type: 'email', autocomplete: 'username', required: '' }));
  const password = field('Senha', input({ type: 'password', autocomplete: 'current-password', required: '' }));
  const feedback = el('div', {});
  const submit = button('Entrar', undefined, { variant: 'primary', type: 'submit' });

  const form = el('form', { class: 'form login__form', novalidate: '' }, email.root, password.root, feedback, submit);

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    if (!email.control.value || !password.control.value) {
      feedback.replaceChildren(message('error', 'Informe e-mail e senha.'));
      return;
    }
    submit.disabled = true;
    submit.textContent = 'Entrando…';
    try {
      session.save(await api.login(email.control.value.trim(), password.control.value));
      onSuccess();
    } catch (cause) {
      feedback.replaceChildren(
        message(
          'error',
          cause instanceof ApiError && cause.status === 401
            ? 'E-mail ou senha incorretos, ou conta bloqueada. Após 5 tentativas erradas, espere 15 minutos.'
            : cause instanceof ApiError && cause.status === 400
              ? 'Confira o e-mail: ele precisa ter o formato nome@dominio.'
              : 'Sem conexão com o servidor. Confira a internet e tente de novo.',
        ),
      );
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
      el('p', { class: 'login__subtitle' }, 'Acesso da equipe do Lar de Margarida'),
      form,
      ...(import.meta.env.VITE_DEMO === 'true' ? [demoNotice()] : []),
    ),
  );
  email.control.focus();
}

// Only the public demo build (VITE_DEMO=true) shows these accounts; scripts/seed-demo.sh creates them.
const DEMO_ACCOUNTS = [
  { role: 'Administração', email: 'admin@zelo.com' },
  { role: 'Coordenação', email: 'coordenador@zelo.com' },
  { role: 'Voluntariado', email: 'voluntario@zelo.com' },
];

function demoNotice(): HTMLElement {
  return el(
    'section',
    { class: 'demo-notice', 'aria-labelledby': 'demo-notice-title' },
    el('h2', { class: 'demo-notice__title', id: 'demo-notice-title' }, icon('circle'), 'Ambiente de demonstração'),
    el(
      'p',
      {},
      'Todos os dados são fictícios. Entre com uma das contas abaixo; a senha de todas é ',
      el('strong', {}, 'password'),
      '.',
    ),
    el(
      'dl',
      { class: 'demo-notice__accounts' },
      ...DEMO_ACCOUNTS.flatMap((account) => [el('dt', {}, account.role), el('dd', {}, account.email)]),
    ),
  );
}
