import { api } from '../api.ts';
import { context, ROLE_LABEL } from '../context.ts';
import { button, errorText, field, input, message, pageHeader, section, showFieldErrors } from '../design-system/ui.ts';
import { el } from '../dom.ts';
import { passwordForm } from './password.ts';

export function renderProfile(main: HTMLElement): void {
  const profile = context.profile;
  const fullName = field('Nome', input({ maxlength: '150', value: profile.fullName }));
  const feedback = el('div', {});
  const form = el(
    'form',
    { class: 'form', novalidate: '' },
    fullName.root,
    feedback,
    el('div', { class: 'form__actions' }, button('Salvar nome', undefined, { variant: 'primary', type: 'submit' })),
  );
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    fullName.setError('');
    try {
      context.set(await api.updateMe(fullName.control.value.trim()));
      const user = document.querySelector('.nav__user');
      if (user) {
        user.textContent = context.profile.fullName;
      }
      feedback.replaceChildren(message('success', 'Nome salvo.'));
    } catch (cause) {
      if (!showFieldErrors(cause, { fullName })) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
    }
  });
  main.replaceChildren(
    pageHeader('Meu perfil', `${profile.email}, ${ROLE_LABEL[profile.role]}`),
    form,
    section('Trocar senha', passwordForm(() => undefined)),
  );
}
