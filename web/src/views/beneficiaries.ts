import {
  api,
  ApiError,
  type Activity,
  type Beneficiary,
  type BeneficiaryDocument,
  type BeneficiaryRequest,
  type Delivery,
} from '../api.ts';
import { context } from '../context.ts';
import { icon } from '../design-system/icons.ts';
import {
  avatar,
  badge,
  button,
  confirmDialog,
  empty,
  errorText,
  field,
  input,
  link,
  loading,
  message,
  openDialog,
  pageHeader,
  row,
  rows,
  section,
  select,
  showFieldErrors,
  textarea,
} from '../design-system/ui.ts';
import { el } from '../dom.ts';
import { formatDate, formatDateTime, today } from '../format.ts';
import { navigate } from '../router.ts';

// Same rule as the API: 11 digits with valid check digits.
export function isValidCpf(cpf: string): boolean {
  if (!/^\d{11}$/.test(cpf) || new Set(cpf).size === 1) {
    return false;
  }
  const digit = (length: number) => {
    let sum = 0;
    for (let i = 0; i < length; i += 1) {
      sum += Number(cpf[i]) * (length + 1 - i);
    }
    const rest = (sum * 10) % 11;
    return rest === 10 ? 0 : rest;
  };
  return digit(9) === Number(cpf[9]) && digit(10) === Number(cpf[10]);
}

function onlyDigits(value: string): string {
  return value.replace(/\D/g, '');
}

// Lists show only the end of the CPF.
function maskedCpf(cpf: string | null): string {
  return cpf ? `•••.•••.${cpf.slice(6, 9)}-${cpf.slice(9)}` : '';
}

function formattedCpf(cpf: string | null): string {
  return cpf ? `${cpf.slice(0, 3)}.${cpf.slice(3, 6)}.${cpf.slice(6, 9)}-${cpf.slice(9)}` : '';
}

function statusBadge(beneficiary: Beneficiary): HTMLElement {
  return beneficiary.status === 'ACTIVE' ? badge('present', '✓', 'Ativo') : badge('neutral', '○', 'Inativo');
}

export async function renderBeneficiaries(main: HTMLElement): Promise<void> {
  const name = field('Nome', input({ type: 'search', autocomplete: 'off' }));
  const document = field('CPF', input({ inputmode: 'numeric', maxlength: '14' }), 'Com ou sem pontos.');
  const activity = field('Turma', select([{ value: '', label: 'Todas' }]));
  const status = field(
    'Situação',
    select(
      [
        { value: 'ALL', label: 'Ativos e inativos' },
        { value: 'ACTIVE', label: 'Só ativos' },
        { value: 'INACTIVE', label: 'Só inativos' },
      ],
      'ALL',
    ),
  );
  const order = field(
    'Ordem',
    select([
      { value: 'ASC', label: 'Nome de A a Z' },
      { value: 'DESC', label: 'Nome de Z a A' },
    ]),
  );
  const results = el('div', { 'aria-live': 'polite' }, loading());
  const form = el(
    'form',
    { class: 'form search-form', role: 'search' },
    el('div', { class: 'form__row' }, name.root, document.root),
    el('div', { class: 'form__row' }, activity.root, status.root, order.root),
    el('div', { class: 'form__actions' }, button('Pesquisar', undefined, { type: 'submit', variant: 'primary', icon: 'search' })),
  );
  const create = context.manages ? link('Cadastrar beneficiário', '#/beneficiarios/novo', 'primary', { icon: 'plus' }) : null;
  main.replaceChildren(
    pageHeader('Beneficiários', 'Crianças e jovens atendidos pela ONG.', ...(create ? [create] : [])),
    form,
    section('Resultado', results),
  );

  api
    .listActivities()
    .then((activities: Activity[]) =>
      activities.forEach((item) => activity.control.append(el('option', { value: String(item.id) }, item.title))),
    )
    .catch(() => undefined);

  const search = async () => {
    results.replaceChildren(loading());
    try {
      const found = await api.searchBeneficiaries({
        name: name.control.value.trim() || undefined,
        documentNumber: onlyDigits(document.control.value) || undefined,
        activityId: activity.control.value ? Number(activity.control.value) : undefined,
        order: order.control.value as 'ASC' | 'DESC',
        status: status.control.value as 'ACTIVE' | 'INACTIVE' | 'ALL',
      });
      results.replaceChildren(
        found.length === 0
          ? empty('Ninguém encontrado com esses filtros.')
          : el(
              'div',
              { class: 'stack' },
              el('p', { class: 'row__meta' }, `${found.length} ${found.length === 1 ? 'pessoa' : 'pessoas'}`),
              rows(
                ...found.map((beneficiary) => {
                  const item = row(
                    [
                      el('a', { class: 'row__title', href: `#/beneficiarios/${beneficiary.id}` }, beneficiary.fullName),
                      el(
                        'span',
                        { class: 'row__meta' },
                        `CPF ${maskedCpf(beneficiary.documentNumber)}, nascimento ${formatDate(beneficiary.birthDate)}`,
                      ),
                    ],
                    [statusBadge(beneficiary), link('Ver ficha', `#/beneficiarios/${beneficiary.id}`, 'secondary', { small: true })],
                    avatar(beneficiary.fullName),
                  );
                  if (beneficiary.status === 'INACTIVE') {
                    item.classList.add('row--inactive');
                  }
                  return item;
                }),
              ),
            ),
      );
    } catch (cause) {
      results.replaceChildren(message('error', errorText(cause)));
    }
  };
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    void search();
  });
  await search();
}

export async function renderBeneficiary(main: HTMLElement, id: number): Promise<void> {
  main.replaceChildren(loading());
  let beneficiary: Beneficiary;
  try {
    beneficiary = await api.getBeneficiary(id);
  } catch (cause) {
    main.replaceChildren(message('error', errorText(cause, 'Beneficiário não encontrado.')));
    return;
  }
  const actions: HTMLElement[] = [];
  const feedback = el('div', {});
  const active = beneficiary.status === 'ACTIVE';
  if (context.manages && active) {
    actions.push(
      link('Editar', `#/beneficiarios/${id}/editar`),
      button('Inativar', async () => {
        const confirmed = await confirmDialog({
          title: `Inativar ${beneficiary.fullName}?`,
          message: 'O cadastro sai das turmas, da chamada e dos alertas, mas continua na lista com a situação "Inativo" e pode ser reativado.',
          confirmLabel: 'Inativar',
          tone: 'danger',
        });
        if (!confirmed) {
          return;
        }
        try {
          await api.deactivateBeneficiary(id);
          void renderBeneficiary(main, id);
        } catch (cause) {
          feedback.replaceChildren(message('error', errorText(cause)));
        }
      }, { variant: 'danger' }),
    );
  }
  if (context.manages && !active) {
    actions.push(
      button('Reativar cadastro', async () => {
        try {
          await api.reactivateBeneficiary(id);
          void renderBeneficiary(main, id);
        } catch (cause) {
          feedback.replaceChildren(message('error', errorText(cause)));
        }
      }, { variant: 'primary', icon: 'refresh' }),
    );
  }
  const facts = el(
    'dl',
    { class: 'facts' },
    ...fact('Situação', statusBadge(beneficiary)),
    ...fact('CPF', formattedCpf(beneficiary.documentNumber)),
    ...fact('Nascimento', formatDate(beneficiary.birthDate)),
    ...fact('Telefone', el('a', { href: `tel:${beneficiary.phoneNumber}` }, beneficiary.phoneNumber)),
    ...fact('Endereço', beneficiary.address ?? 'Não informado'),
  );
  main.replaceChildren(
    pageHeader(beneficiary.fullName, undefined, ...actions),
    ...(active ? [] : [message('info', 'Cadastro inativo: não aparece nas turmas, na chamada nem nos alertas.')]),
    feedback,
    facts,
  );

  if (context.manages) {
    const documents = el('div', {}, loading());
    const deliveries = el('div', {}, loading());
    main.append(section('Documentos', documents), section('Assistência recebida', deliveries));
    void documentsSection(documents, id);
    try {
      const list: Delivery[] = await api.listBeneficiaryDeliveries(id);
      deliveries.replaceChildren(
        list.length === 0
          ? empty('Nenhuma entrega registrada para esta pessoa.')
          : rows(
              ...list.map((delivery) =>
                row([
                  el('span', { class: 'row__title' }, `${delivery.quantity} × ${delivery.itemName}`),
                  el(
                    'span',
                    { class: 'row__meta' },
                    `${formatDate(delivery.deliveredAt)}${delivery.recordedBy ? `, registrado por ${delivery.recordedBy}` : ''}`,
                  ),
                ]),
              ),
            ),
      );
    } catch (cause) {
      deliveries.replaceChildren(message('error', errorText(cause)));
    }
  }
}

function fact(term: string, value: string | Node): HTMLElement[] {
  return [el('dt', {}, term), el('dd', {}, value)];
}

function formatSize(bytes: number): string {
  return bytes < 1024 * 1024 ? `${Math.max(1, Math.round(bytes / 1024))} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

const MAX_DOCUMENT_BYTES = 5 * 1024 * 1024;

async function documentsSection(container: HTMLElement, beneficiaryId: number): Promise<void> {
  let documents: BeneficiaryDocument[];
  try {
    documents = await api.listDocuments(beneficiaryId);
  } catch (cause) {
    container.replaceChildren(message('error', errorText(cause)));
    return;
  }
  const reload = () => void documentsSection(container, beneficiaryId);

  const list =
    documents.length === 0
      ? empty('Nenhum documento anexado.')
      : rows(
          ...documents.map((document) =>
            row(
              [
                el('span', { class: 'row__title' }, document.title),
                el(
                  'span',
                  { class: 'row__meta' },
                  `${document.fileName}, ${formatSize(document.sizeBytes)}, enviado em ${formatDateTime(document.uploadedAt)}`,
                ),
              ],
              [
                button('Baixar', async () => {
                  try {
                    const blob = await api.downloadDocument(beneficiaryId, document.id);
                    const url = URL.createObjectURL(blob);
                    const anchor = el('a', { href: url, download: document.fileName });
                    anchor.click();
                    setTimeout(() => URL.revokeObjectURL(url), 1000);
                  } catch (cause) {
                    container.prepend(message('error', errorText(cause)));
                  }
                }, { small: true, icon: 'download' }),
                button('Excluir', async () => {
                  const confirmed = await confirmDialog({
                    title: 'Excluir documento?',
                    message: `"${document.title}" será apagado definitivamente.`,
                    confirmLabel: 'Excluir',
                    tone: 'danger',
                  });
                  if (confirmed) {
                    await api.deleteDocument(beneficiaryId, document.id).catch((cause) =>
                      container.prepend(message('error', errorText(cause))),
                    );
                    reload();
                  }
                }, { small: true, variant: 'ghost', icon: 'trash' }),
              ],
              el('span', { class: 'file-icon', 'aria-hidden': 'true' }, icon('file')),
            ),
          ),
        );

  const title = field('Tipo de documento', input({ maxlength: '100', list: 'document-titles' }), 'Ex.: comprovante de residência, diploma.');
  const suggestions = el(
    'datalist',
    { id: 'document-titles' },
    ...['Comprovante de residência', 'Certidão de nascimento', 'Diploma', 'Declaração escolar', 'Documento do responsável'].map(
      (value) => el('option', { value }),
    ),
  );
  const file = field('Arquivo', input({ type: 'file', accept: 'application/pdf,image/jpeg,image/png' }), 'PDF, JPG ou PNG, até 5 MB.');
  const feedback = el('div', {});
  const upload = el(
    'form',
    { class: 'form upload-form', novalidate: '' },
    el('div', { class: 'form__row' }, title.root, file.root),
    suggestions,
    feedback,
    el('div', { class: 'form__actions' }, button('Anexar documento', undefined, { type: 'submit', variant: 'primary', icon: 'upload' })),
  );
  upload.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    title.setError('');
    file.setError('');
    const chosen = file.control.files?.[0];
    if (!title.control.value.trim()) {
      title.setError('Diga que documento é este.');
      return;
    }
    if (!chosen) {
      file.setError('Escolha um arquivo.');
      return;
    }
    if (chosen.size > MAX_DOCUMENT_BYTES) {
      file.setError('O arquivo passa de 5 MB.');
      return;
    }
    try {
      await api.uploadDocument(beneficiaryId, title.control.value.trim(), chosen);
      reload();
    } catch (cause) {
      feedback.replaceChildren(
        message(
          'error',
          cause instanceof ApiError && cause.status === 400
            ? 'Só são aceitos arquivos PDF, JPG ou PNG.'
            : cause instanceof ApiError && cause.status === 413
              ? 'O arquivo passa de 5 MB.'
              : errorText(cause),
        ),
      );
    }
  });
  container.replaceChildren(list, upload);
}

export async function renderBeneficiaryForm(main: HTMLElement, id?: number): Promise<void> {
  let beneficiary: Beneficiary | null = null;
  if (id) {
    main.replaceChildren(loading());
    try {
      beneficiary = await api.getBeneficiary(id);
    } catch (cause) {
      main.replaceChildren(message('error', errorText(cause, 'Beneficiário não encontrado.')));
      return;
    }
  }
  const fullName = field('Nome completo', input({ maxlength: '150', value: beneficiary?.fullName ?? '' }));
  const documentNumber = field(
    'CPF',
    input({ inputmode: 'numeric', maxlength: '14', autocomplete: 'off', value: formattedCpf(beneficiary?.documentNumber ?? null) }),
    'Obrigatório. Cada CPF só pode ser cadastrado uma vez.',
  );
  const birthDate = field('Data de nascimento', input({ type: 'date', max: today(), value: beneficiary?.birthDate ?? '' }));
  const phoneNumber = field(
    'Telefone para contato',
    input({ type: 'tel', maxlength: '20', value: beneficiary?.phoneNumber ?? '' }),
    'Usado para a busca ativa quando a criança falta.',
  );
  const address = field('Endereço', textarea(beneficiary?.address ?? ''), 'Opcional.');
  const feedback = el('div', {});
  const save = button(beneficiary ? 'Salvar alterações' : 'Cadastrar', undefined, { variant: 'primary', type: 'submit' });
  const form = el(
    'form',
    { class: 'form', novalidate: '' },
    fullName.root,
    el('div', { class: 'form__row' }, documentNumber.root, birthDate.root),
    phoneNumber.root,
    address.root,
    feedback,
    el(
      'div',
      { class: 'form__actions' },
      save,
      link('Cancelar', beneficiary ? `#/beneficiarios/${beneficiary.id}` : '#/beneficiarios', 'ghost'),
    ),
  );
  const fields = { fullName, birthDate, phoneNumber, documentNumber, address };
  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    feedback.replaceChildren();
    Object.values(fields).forEach((item) => item.setError(''));
    const cpf = onlyDigits(documentNumber.control.value);
    if (!isValidCpf(cpf)) {
      documentNumber.setError('CPF inválido. Confira os 11 números.');
      documentNumber.control.focus();
      return;
    }
    const body: BeneficiaryRequest = {
      fullName: fullName.control.value.trim(),
      birthDate: birthDate.control.value,
      phoneNumber: phoneNumber.control.value.trim(),
      documentNumber: cpf,
      address: address.control.value.trim() || null,
    };
    save.disabled = true;
    try {
      const saved = beneficiary
        ? await api.updateBeneficiary(beneficiary.id, body)
        : await api.createBeneficiary(body);
      navigate(`/beneficiarios/${saved.id}`);
    } catch (cause) {
      save.disabled = false;
      if (cause instanceof ApiError && cause.status === 409 && typeof cause.problem.beneficiaryId === 'number') {
        showDuplicate(cause.problem);
        return;
      }
      if (!showFieldErrors(cause, fields)) {
        feedback.replaceChildren(message('error', errorText(cause)));
      }
    }
  });
  main.replaceChildren(
    pageHeader(beneficiary ? `Editar ${beneficiary.fullName}` : 'Cadastrar beneficiário'),
    form,
  );
  fullName.control.focus();
}

// One CPF, one record: the dialog shows who already has it and, when inactive, offers to reactivate.
function showDuplicate(problem: Record<string, unknown>): void {
  const id = problem.beneficiaryId as number;
  const fullName = String(problem.fullName ?? '');
  const inactive = problem.status === 'INACTIVE';
  openDialog({
    title: 'Este CPF já está cadastrado',
    icon: 'idCard',
    tone: 'warning',
    body: [
      el(
        'p',
        {},
        inactive
          ? 'Encontramos um cadastro inativo com o mesmo CPF. Reative-o em vez de criar outro.'
          : 'Já existe um cadastro ativo com o mesmo CPF.',
      ),
      el(
        'div',
        { class: 'dialog__person' },
        avatar(fullName),
        el('span', { class: 'row__title' }, fullName),
        inactive ? badge('neutral', '○', 'Inativo') : badge('present', '✓', 'Ativo'),
      ),
    ],
    actions: (close) => [
      button('Cancelar', close),
      inactive
        ? button('Reativar cadastro', async () => {
            try {
              await api.reactivateBeneficiary(id);
              close();
              navigate(`/beneficiarios/${id}`);
            } catch (cause) {
              close();
              openDialog({ title: 'Não foi possível reativar', body: [errorText(cause)], actions: (done) => [button('Fechar', done)] });
            }
          }, { variant: 'primary', icon: 'refresh' })
        : button('Abrir cadastro', () => {
            close();
            navigate(`/beneficiarios/${id}`);
          }, { variant: 'primary' }),
    ],
  });
}
