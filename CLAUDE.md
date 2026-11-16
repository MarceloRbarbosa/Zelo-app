# CLAUDE.md — Zelo

Regras para trabalhar neste repositório. O que o produto faz (requisitos, modelo de dados e
permissões) está em [`docs/requisitos.md`](docs/requisitos.md); o contrato da API, em
[`docs/api.md`](docs/api.md).

## Produto
**Zelo** (`Zelo-app`): plataforma PWA de gestão operacional, assiduidade e combate à evasão para o
Terceiro Setor (ODS 4). Piloto com a ONG **Lar de Margarida** (Tamoios, Cabo Frio - RJ), usada em
celulares intermediários com rede 4G/Wi-Fi instável.

## Stack e estrutura
- Monorepo: monólito modular por funcionalidade no back-end e front-end desacoplado via API REST (JSON).
- `api/`: Java 25, Spring Boot 4.1.x, Spring Data JPA, Spring Security (JWT stateless + BCrypt), Flyway, HikariCP.
- `web/`: PWA em TypeScript (`strict: true`) com Vite, sem framework de UI; `manifest.json` e Service Worker.
- `docs/`: especificação do produto e contrato da API.
- Banco: PostgreSQL 16+ (Supabase); local via `compose.yaml`.

## Módulos do back-end (`br.com.zelo.platform`)
- `shared/` — configuração, `@ControllerAdvice` com `ProblemDetail`, filtros JWT e RBAC.
- `identity/` — login, troca de senha, perfil e equipe.
- `beneficiary/` — cadastro e pesquisa de beneficiários.
- `program/` — turmas, horários, responsáveis, matrículas, diário de classe e alertas de evasão.
- `event/` — eventos, responsáveis, inscrição e presença.
- `assistance/` — itens de assistência, entregas a beneficiários e recebimentos.
- `organization/` — gerenciamento reservado ao admin (remoção de dados pessoais a pedido, LGPD).

Tela que só combina dados de outros módulos (ex.: calendário) não vira módulo.

## Comandos
- Banco local: `docker compose up -d`
- API: `cd api && ./mvnw spring-boot:run` (exige `JWT_SECRET`, `ADMIN_EMAIL`, `ADMIN_PASSWORD`)
- Testes unitários e de integração (exige Docker): `cd api && ./mvnw clean verify`
- Build do front: `npm --prefix web run build`
- Dados de demonstração (API no ar, banco vazio): `ADMIN_EMAIL=... ADMIN_PASSWORD=... scripts/seed-demo.sh`

## Regras obrigatórias
**Banco**
- Schema só por migrations Flyway em `api/src/main/resources/db/migration/`. Nunca `ddl-auto: update`.

**Segurança**
- JWT stateless (`Authorization: Bearer`, 8h), BCrypt fator 10, bloqueio após 5 falhas de login.
- `@PreAuthorize` em todo endpoint, seguindo a matriz de `docs/requisitos.md`.
- Status do membro conferido a cada requisição: bloqueio vale na hora.
- Consultas parametrizadas via JPA; erros só como `ProblemDetail`, sem stack trace.
- No front, dado dinâmico só com `textContent`; `innerHTML` com dado dinâmico é proibido.
- Segredos e credenciais por variável de ambiente (`${ENV_VAR}`).

**Desempenho**
- Payloads JSON enxutos; índices compostos nas consultas por turma e beneficiário. Meta: tela em < 3,0 s em rede móvel lenta.

**Front-end e acessibilidade (WCAG 2.1 AA)**
- Design system próprio em `web/src/design-system/`: tokens em CSS (cor, tipografia, espaçamento), componentes reutilizáveis e ícones SVG próprios. Sem biblioteca de UI.
- Visual de ONG: verde-floresta para estrutura e ações, girassol para destaques, base neutra; uma cor por turma na agenda; cor só com significado, sempre com ícone e texto. Sem fotos de crianças.
- Confirmações e avisos usam o modal do design system (`openDialog`/`confirmDialog`), nunca `alert`/`confirm` do navegador.
- Fonte Figtree (licença OFL) servida de `web/src/design-system/fonts/`: única exceção à regra de não adicionar dependência.
- Mobile-first (360px); alvos de toque ≥ 48×48px com espaçamento ≥ 8px.
- Alertas multimodais: cor + ícone/borda + texto (`Presente`, `Ausente`, `Risco de Evasão`).
- Interfaces TypeScript espelham os DTOs do back-end.

## Convenções
- Código, nomes e valores de domínio em inglês (`ACTIVE`, `LEFT`); textos da tela em pt-BR (`Cursando`, `Saiu`).
- Commits semânticos em inglês (`feat:`, `fix:`, `chore:`, `docs:`, `test:`). README, `docs/` e PR em pt-BR.
- Commit direto na `main`. Se houver PR: branch `<número>-<nome-em-inglês>` e merge sem squash.
- README enxuto; o detalhe fica em `docs/`.
- Services com testes unitários (JUnit 5 + Mockito).
