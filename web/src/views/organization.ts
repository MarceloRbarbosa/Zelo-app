import { api, type Beneficiary, type DataRemoval } from '../api.ts';
import { context } from '../context.ts';
import {
  avatar,
  button,
  empty,
  errorText,
  field,
  loading,
  message,
  openDialog,
  pageHeader,
  section,
  textarea,
} from '../design-system/ui.ts';
import { el } from '../dom.ts';
import { formatDate, formatDateTime } from '../format.ts';
import { beneficiaryPicker } from './picker.ts';

// Organization management, reserved to admins. For now: removing a beneficiary's personal data on request (LGPD).
export async function renderOrganization(main: HTMLElement): Promise<void> {
  if (!context.is('ROLE_ADMIN')) {
    main.replaceChildren(message('error', 'Só a administração acessa o gerenciamento da organização.'));
    return;
  }
  const history = el('div', {}, loading());
  main.replaceChildren(
    pageHeader('Organização', 'Gerenciamento reservado à administração da ONG.'),
    section(
      'Remoção de dados pessoais (LGPD)',
      el(
        'p',
        { class: 'row__meta' },
        'Use somente quando o beneficiário ou o responsável pedir. Nome, CPF, nascimento, telefone, endereço e documentos ',
        'são apagados para sempre; as chamadas e entregas continuam contadas, sem identificar a pessoa.',
      ),
      beneficiaryPicker('Remover dados', async (beneficiary) => confirmRemoval(beneficiary, () => void loadHistory(history)), () => false, 'ALL'),
    ),
    section('Histórico de remoções', history),
  );
  await loadHistory(history);
}

function confirmRemoval(beneficiary: Beneficiary, onDone: () => void): Promise<boolean> {
  return new Promise((resolve) => {
    let removed = false;
    const note = field(
      'Quem pediu e como',
      textarea(),
      'Ex.: "pedido da mãe, por escrito, em 16/11". Não escreva dados pessoais aqui.',
    );
    const feedback = el('div', {});
    const { element } = openDialog({
      title: `Remover os dados de ${beneficiary.fullName}?`,
      icon: 'shield',
      tone: 'danger',
      body: [
        el(
          'div',
          { class: 'dialog__person' },
          avatar(beneficiary.fullName),
          el(
            'div',
            { class: 'row__main' },
            el('span', { class: 'row__title' }, beneficiary.fullName),
            el('span', { class: 'row__meta' }, `Nascimento ${formatDate(beneficiary.birthDate)}`),
          ),
        ),
        el('p', {}, 'Esta ação não pode ser desfeita.'),
        note.root,
        feedback,
      ],
      actions: (close) => [
        button('Cancelar', close),
        button('Remover dados', async () => {
          note.setError('');
          if (!note.control.value.trim()) {
            note.setError('Registre quem pediu a remoção.');
            note.control.focus();
            return;
          }
          try {
            await api.removeBeneficiaryData(beneficiary.id, note.control.value.trim());
            removed = true;
            close();
            onDone();
          } catch (cause) {
            feedback.replaceChildren(message('error', errorText(cause)));
          }
        }, { variant: 'danger-solid', icon: 'trash' }),
      ],
    });
    element.addEventListener('close', () => resolve(removed));
  });
}

async function loadHistory(container: HTMLElement): Promise<void> {
  let removals: DataRemoval[];
  try {
    removals = await api.listDataRemovals();
  } catch (cause) {
    container.replaceChildren(message('error', errorText(cause)));
    return;
  }
  container.replaceChildren(
    removals.length === 0
      ? empty('Nenhuma remoção feita até agora.')
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
                el('th', { scope: 'col' }, 'Data'),
                el('th', { scope: 'col' }, 'Registro'),
                el('th', { scope: 'col' }, 'Feita por'),
                el('th', { scope: 'col' }, 'Pedido'),
              ),
            ),
            el(
              'tbody',
              {},
              ...removals.map((removal) =>
                el(
                  'tr',
                  {},
                  el('td', {}, formatDateTime(removal.removedAt)),
                  el('td', {}, `Beneficiário nº ${removal.beneficiaryId}`),
                  el('td', {}, removal.removedBy ?? '—'),
                  el('td', {}, removal.note),
                ),
              ),
            ),
          ),
        ),
  );
}
