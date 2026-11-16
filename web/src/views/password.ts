import { api } from '../api.ts';
import { button, errorText, field, input, message, showFieldErrors } from '../design-system/ui.ts';
import { el } from '../dom.ts';

// Shared by the first-login screen and the profile page.
export function passwordForm(onChanged: () => void): HTMLFormElement {
  const current = field('Senha atual', input({ type: 'password', autocomplete: 'current-password' }));
  const next = field(
    'Nova senha',
    input({ type: 'password', autocomplete: 'new-password' }),
    'De 8 a 72 caracteres, diferente da atual.',
  );
  const repeat = field('Repita a nova senha', input({ type: 'password', autocomplete: 'new-password' }));
  const feedback = el('div', {});
  const submit = button('Trocar senha', undefined, { variant: 'primary', type: 'submit' });
  const form = el('form', { class: 'form', novalidate: '' }, current.root, next.root, repeat.root, feedback, submit);

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    [current, next, repeat].forEach((item) => item.setError(''));
    if (next.control.value.length < 8) {
      next.setError('Use pelo menos 8 caracteres.');
      return;
    }
    if (next.control.value !== repeat.control.value) {
      repeat.setError('As senhas não conferem.');
      return;
    }
    submit.disabled = true;
    try {
      await api.changePassword(current.control.value, next.control.value);
      form.reset();
      feedback.replaceChildren(message('success', 'Senha trocada.'));
      onChanged();
    } catch (cause) {
      if (!showFieldErrors(cause, { currentPassword: current, newPassword: next })) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
    } finally {
      submit.disabled = false;
    }
  });
  return form;
}

export function renderFirstPassword(root: HTMLElement, onChanged: () => void, onLogout: () => void): void {
  const logout = button('Sair', onLogout, { variant: 'ghost' });
  root.replaceChildren(
    el(
      'main',
      { class: 'login' },
      el('img', { class: 'login__logo', src: '/logo.jpg', alt: 'Girassol, logotipo do Lar de Margarida' }),
      el('h1', { class: 'login__title' }, 'Crie sua senha'),
      el(
        'p',
        { class: 'login__subtitle' },
        'Você entrou com a senha provisória. Para continuar, troque por uma senha só sua.',
      ),
      el('div', { class: 'login__form' }, passwordForm(onChanged)),
      logout,
    ),
  );
}
