import './design-system/tokens.css';
import './design-system/components.css';
import './design-system/layout.css';
import './pages.css';
import { api, session, UNAUTHORIZED_EVENT } from './api.ts';
import { context } from './context.ts';
import { markCurrent, renderShell } from './shell.ts';
import { renderActivities, renderActivity, renderActivityForm } from './views/activities.ts';
import { renderAssistance } from './views/assistance.ts';
import { renderBeneficiaries, renderBeneficiary, renderBeneficiaryForm } from './views/beneficiaries.ts';
import { renderCalendar } from './views/calendar.ts';
import { renderEvent, renderEventForm, renderEvents } from './views/events.ts';
import { renderHome } from './views/home.ts';
import { renderLogin } from './views/login.ts';
import { renderFirstPassword } from './views/password.ts';
import { renderProfile } from './views/profile.ts';
import { renderTeam } from './views/team.ts';
import { message } from './design-system/ui.ts';
import { match } from './router.ts';

const root = document.querySelector<HTMLDivElement>('#app');
if (!root) {
  throw new Error('#app element not found');
}
const app: HTMLDivElement = root;

type View = (main: HTMLElement, params: Record<string, string>) => void | Promise<void>;

const ROUTES: [string, View][] = [
  ['/', (main) => renderHome(main)],
  ['/turmas', (main) => renderActivities(main)],
  ['/turmas/nova', (main) => renderActivityForm(main)],
  ['/turmas/:id', (main, params) => renderActivity(main, Number(params.id))],
  ['/turmas/:id/editar', (main, params) => renderActivityForm(main, Number(params.id))],
  ['/beneficiarios', (main) => renderBeneficiaries(main)],
  ['/beneficiarios/novo', (main) => renderBeneficiaryForm(main)],
  ['/beneficiarios/:id', (main, params) => renderBeneficiary(main, Number(params.id))],
  ['/beneficiarios/:id/editar', (main, params) => renderBeneficiaryForm(main, Number(params.id))],
  ['/eventos', (main) => renderEvents(main)],
  ['/eventos/novo', (main) => renderEventForm(main)],
  ['/eventos/:id', (main, params) => renderEvent(main, Number(params.id))],
  ['/eventos/:id/editar', (main, params) => renderEventForm(main, Number(params.id))],
  ['/calendario', (main) => renderCalendar(main)],
  ['/assistencia', (main) => renderAssistance(main)],
  ['/equipe', (main) => renderTeam(main)],
  ['/perfil', (main) => renderProfile(main)],
];

let main: HTMLElement | null = null;

function logout(): void {
  session.clear();
  context.set(null);
  main = null;
  location.hash = '/';
  start();
}

function route(): void {
  if (!main) {
    return;
  }
  const path = location.hash.replace(/^#/, '') || '/';
  markCurrent(path);
  for (const [pattern, view] of ROUTES) {
    const params = match(pattern, path);
    if (params) {
      main.replaceChildren();
      void view(main, params);
      main.focus();
      window.scrollTo(0, 0);
      return;
    }
  }
  main.replaceChildren(message('error', 'Página não encontrada. Use o menu para continuar.'));
}

async function start(): Promise<void> {
  if (!session.token()) {
    renderLogin(app, start);
    return;
  }
  try {
    context.set(await api.me());
  } catch {
    session.clear();
    renderLogin(app, start);
    return;
  }
  if (context.profile.mustChangePassword) {
    renderFirstPassword(app, start, logout);
    return;
  }
  main = renderShell(app, logout);
  route();
}

window.addEventListener('hashchange', route);
window.addEventListener(UNAUTHORIZED_EVENT, () => {
  main = null;
  void start();

// Service worker only in the built app; in development Vite serves files that change all the time.
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  void navigator.serviceWorker.register('/sw.js');
}
});
void start();

// Service worker only in the built app; in development Vite serves files that change all the time.
if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  void navigator.serviceWorker.register('/sw.js');
}
