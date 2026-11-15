import './style.css';
import { session, UNAUTHORIZED_EVENT } from './api.ts';
import { renderAttendance } from './views/attendance.ts';
import { renderLogin } from './views/login.ts';

const root = document.querySelector<HTMLDivElement>('#app');
if (!root) {
  throw new Error('#app element not found');
}
const app: HTMLDivElement = root;

function logout(): void {
  session.clear();
  render();
}

function render(): void {
  if (!session.token()) {
    renderLogin(app, render);
    return;
  }
  void renderAttendance(app, logout);
}

window.addEventListener(UNAUTHORIZED_EVENT, render);
render();
