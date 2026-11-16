import { api, type Beneficiary } from '../api.ts';
import { button, empty, errorText, field, input, message, row, rows } from '../design-system/ui.ts';
import { el } from '../dom.ts';
import { formatDate } from '../format.ts';

// Searches every registered beneficiary by name or document; only people already registered can be picked.
export function beneficiaryPicker(
  actionLabel: string,
  onPick: (beneficiary: Beneficiary) => Promise<void>,
  isTaken: (id: number) => boolean = () => false,
): HTMLElement {
  const term = field('Nome ou documento', input({ type: 'search', autocomplete: 'off' }));
  const results = el('div', { 'aria-live': 'polite' });
  const search = button('Buscar', undefined, { type: 'submit' });

  const runSearch = async () => {
    const value = term.control.value.trim();
    if (value.length < 2) {
      results.replaceChildren(message('info', 'Digite pelo menos 2 letras do nome ou o documento completo.'));
      return;
    }
    results.replaceChildren(el('p', { role: 'status' }, 'Buscando…'));
    try {
      const found = await api.searchBeneficiaries(
        /^\d{11}$/.test(value) ? { documentNumber: value } : { name: value },
      );
      if (found.length === 0) {
        results.replaceChildren(empty('Ninguém encontrado. A pessoa precisa estar cadastrada em Beneficiários.'));
        return;
      }
      results.replaceChildren(
        rows(
          ...found.map((beneficiary) => {
            const taken = isTaken(beneficiary.id);
            const action = button(taken ? 'Já incluído' : actionLabel, undefined, { small: true });
            action.disabled = taken;
            action.addEventListener('click', async () => {
              action.disabled = true;
              try {
                await onPick(beneficiary);
                action.textContent = 'Incluído';
              } catch (cause) {
                results.prepend(message('error', errorText(cause)));
                action.disabled = false;
              }
            });
            return row(
              [
                el('span', { class: 'row__title' }, beneficiary.fullName),
                el('span', { class: 'row__meta' }, `Nascimento ${formatDate(beneficiary.birthDate)}`),
              ],
              [action],
            );
          }),
        ),
      );
    } catch (cause) {
      results.replaceChildren(message('error', errorText(cause)));
    }
  };

  const form = el('form', { class: 'picker', role: 'search' }, term.root, search);
  form.addEventListener('submit', (event) => {
    event.preventDefault();
    void runSearch();
  });
  return el('div', { class: 'picker__wrap' }, form, results);
}
