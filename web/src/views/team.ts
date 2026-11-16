import { api, type Role, type StaffMember } from '../api.ts';
import { context, ROLE_LABEL } from '../context.ts';
import {
  avatar,
  badge,
  button,
  errorText,
  field,
  input,
  loading,
  message,
  pageHeader,
  row,
  rows,
  section,
  select,
  showFieldErrors,
} from '../design-system/ui.ts';
import { el } from '../dom.ts';

const MEMBER_TYPE: Record<StaffMember['memberType'], string> = {
  VOLUNTEER: 'Voluntário(a)',
  PROFESSIONAL: 'Profissional',
  COORDINATOR: 'Coordenador(a)',
};

// Admin only: register members with a provisional password and block access at once.
export async function renderTeam(main: HTMLElement): Promise<void> {
  if (!context.is('ROLE_ADMIN')) {
    main.replaceChildren(message('error', 'Só a administração gerencia a equipe.'));
    return;
  }
  const list = el('div', {}, loading());
  main.replaceChildren(
    pageHeader('Equipe', 'Quem acessa o Zelo. Bloquear tira o acesso na hora.'),
    list,
    section('Cadastrar membro', memberForm(() => void renderTeam(main))),
  );
  try {
    const staff = await api.listStaff();
    list.replaceChildren(
      rows(
        ...staff.map((member) => {
          const actions: HTMLElement[] = [];
          if (member.id !== context.profile.id) {
            actions.push(
              member.status === 'ACTIVE'
                ? button('Bloquear', async () => {
                    if (!window.confirm(`Bloquear ${member.fullName}? O acesso termina na hora.`)) {
                      return;
                    }
                    await change(() => api.blockStaff(member.id));
                  }, { small: true, variant: 'danger' })
                : button('Desbloquear', () => void change(() => api.unblockStaff(member.id)), { small: true }),
            );
          }
          const status =
            member.status === 'ACTIVE' ? badge('present', '●', 'Ativo') : badge('absent', '✗', 'Bloqueado');
          return row(
            [
              el('span', { class: 'row__title' }, member.fullName, ' ', status),
              el(
                'span',
                { class: 'row__meta' },
                `${member.email}, ${ROLE_LABEL[member.role]}, ${MEMBER_TYPE[member.memberType]}`,
              ),
              ...(member.mustChangePassword
                ? [el('span', { class: 'row__meta' }, 'Ainda não trocou a senha provisória.')]
                : []),
            ],
            actions,
            avatar(member.fullName),
          );
        }),
      ),
    );
  } catch (cause) {
    list.replaceChildren(message('error', errorText(cause)));
  }

  async function change(call: () => Promise<StaffMember>): Promise<void> {
    try {
      await call();
      void renderTeam(main);
    } catch (cause) {
      list.prepend(message('error', errorText(cause)));
    }
  }
}

function memberForm(onCreated: () => void): HTMLFormElement {
  const fullName = field('Nome completo', input({ maxlength: '150' }));
  const email = field('E-mail', input({ type: 'email', maxlength: '100' }), 'É o login da pessoa.');
  const password = field(
    'Senha provisória',
    input({ type: 'text', maxlength: '72', autocomplete: 'off' }),
    'De 8 a 72 caracteres. A pessoa troca no primeiro acesso. Passe a senha pessoalmente.',
  );
  const role = field(
    'Perfil de acesso',
    select(
      (Object.keys(ROLE_LABEL) as Role[]).map((value) => ({ value, label: ROLE_LABEL[value] })),
      'ROLE_VOLUNTEER',
    ),
  );
  const memberType = field(
    'Vínculo',
    select(
      (Object.keys(MEMBER_TYPE) as StaffMember['memberType'][]).map((value) => ({ value, label: MEMBER_TYPE[value] })),
      'VOLUNTEER',
    ),
  );
  const feedback = el('div', {});
  const form = el(
    'form',
    { class: 'form', novalidate: '' },
    fullName.root,
    email.root,
    password.root,
    el('div', { class: 'form__row' }, role.root, memberType.root),
    feedback,
    el('div', { class: 'form__actions' }, button('Cadastrar membro', undefined, { variant: 'primary', type: 'submit' })),
  );
  const fields = { fullName, email, password, role, memberType };
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    Object.values(fields).forEach((item) => item.setError(''));
    if (password.control.value.length < 8) {
      password.setError('Use pelo menos 8 caracteres.');
      return;
    }
    try {
      await api.createStaff({
        fullName: fullName.control.value.trim(),
        email: email.control.value.trim(),
        password: password.control.value,
        role: role.control.value as Role,
        memberType: memberType.control.value as StaffMember['memberType'],
      });
      onCreated();
    } catch (cause) {
      if (!showFieldErrors(cause, fields)) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
    }
  });
  return form;
}
