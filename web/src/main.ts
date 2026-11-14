const app = document.querySelector<HTMLDivElement>('#app');

if (app) {
  const title = document.createElement('h1');
  title.textContent = 'Zelo';
  app.append(title);
}
