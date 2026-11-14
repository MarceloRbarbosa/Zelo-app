# CLAUDE.md — Diretrizes Arquiteturais e Regras do Projeto (Zelo)

## 1. Visão Geral e Propósito
- **Produto:** **Zelo** (`Zelo-app`) — Plataforma SaaS/PWA de gestão operacional, controle de assiduidade e combate à evasão socioeducativa para o Terceiro Setor (ODS 4 — Educação de Qualidade).
- **Contexto Piloto:** Validado inicialmente com a ONG **Lar de Margarida** (Tamoios, Cabo Frio - RJ), operando em dispositivos móveis intermediários e redes 4G/Wi-Fi oscilantes.

## 2. Arquitetura e Stack Tecnológica
- **Padrão:** Monorepo com **Monólito Modular (Package-by-Feature)** no back-end e front-end desacoplado via API REST (JSON).
- **Estrutura de Diretórios:**
  - `api/`: **Java 25**, **Spring Boot 4.1.x**, Spring Data JPA (Hibernate), Spring Security (JWT Stateless + BCrypt), Flyway Migrations e pool HikariCP.
  - `web/`: Front-end PWA construído com **TypeScript**, HTML5 e CSS3 modular (vite ou build leve sem frameworks pesados), garantindo tipagem forte de ponta a ponta, `manifest.json` e Service Worker.
  - `docs/`: Modelagem de dados e especificações técnicas.
- **Banco de Dados:** **PostgreSQL 16+** (Supabase), evoluindo de forma incremental via scripts SQL versionados no Flyway (`src/main/resources/db/migration/`). **Nunca usar `ddl-auto: update` em produção.**

## 3. Organização de Pacotes no Back-end (`br.com.zelo.platform`)
- `shared/`: Configurações transversais (`config/`), tratamento global de exceções com `@ControllerAdvice` e `ProblemDetail` (`exception/`), filtros JWT, `BCryptPasswordEncoder` e regras RBAC (`security/`).
- `identity/`: Autenticação (`/api/v1/auth/login`), controle de permissões (`Role`) e gestão de equipe (`StaffMember`).
- `beneficiary/`: Cadastro e acompanhamento de beneficiários (`Beneficiary`) — **RF01**.
- `program/`: Gestão de atividades recorrentes (`Activity`), eventos pontuais (`Event`), chamada digital (`Attendance`) e motor de alertas de evasão — **RF02** e **RF03**.

## 4. Modelo de Dados Relacional Evolutivo (Inglês)
O schema deve ser construído de forma incremental (V1 para o Core MVP, V2+ para eventos e permissões granulares):

- **`staff_members`** (Profissionais e Voluntários):
  - `id` (BigInt PK), `full_name` (Varchar 150, not null), `email` (Varchar 100, unique, not null), `password_hash` (Varchar 255, BCrypt), `member_type` (`VOLUNTEER`, `PROFESSIONAL`, `COORDINATOR`), `role` (`ROLE_ADMIN`, `ROLE_COORDINATOR`, `ROLE_VOLUNTEER`, `ROLE_BENEFICIARY`), `status` (`ACTIVE`/`INACTIVE`), `created_at` (Timestamp).
- **`beneficiaries`** (Assistidos / Público atendido):
  - `id` (BigInt PK), `full_name` (Varchar 150, not null), `document_number` (Varchar 11, unique quando preenchido), `birth_date` (Date, not null), `phone_number` (Varchar 20, not null — obrigatório para busca ativa), `address` (Varchar 255, nullable), `status` (`ACTIVE`/`INACTIVE` — Soft Delete), `created_at` (Timestamp).
- **`activities`** (Atividades/Oficinas Recorrentes):
  - `id` (BigInt PK), `title` (Varchar 100, not null), `description` (Varchar 255, nullable), `recurrence_pattern` (Varchar 50 — ex.: `WEEKLY_MON_WED`), `lead_staff_id` (FK -> `staff_members`), `status` (`ACTIVE`/`FINISHED`), `start_date` (Date, not null).
- **`events`** (Ações Pontuais / Seminários — Não recorrentes):
  - `id` (BigInt PK), `title` (Varchar 150, not null), `description` (Text, nullable), `event_date` (Timestamp, not null), `location` (Varchar 150, nullable), `organizer_id` (FK -> `staff_members`), `status` (`SCHEDULED`/`COMPLETED`/`CANCELED`).
- **`attendances`** (Registro de Frequência):
  - `id` (BigInt PK), `activity_id` (FK -> `activities`, nullable), `event_id` (FK -> `events`, nullable), `beneficiary_id` (FK -> `beneficiaries`, not null), `attendance_date` (Date, not null), `is_present` (Boolean, not null), `notes` (Varchar 255, sanitized).
  - **Restrições Críticas:** `UNIQUE (activity_id, beneficiary_id, attendance_date)` e regra garantindo que `activity_id` ou `event_id` esteja preenchido.

## 5. Escopo Funcional Priorizado
- **RF01 (Core MVP):** CRUD de `Beneficiary` com validação de campos obrigatórios, unicidade de documento e Soft Delete.
- **RF02 (Core MVP):** Lançamento em lote da chamada diária (`POST /api/v1/attendances/batch`) vinculada a uma `Activity`.
- **RF03 (Core MVP):** Serviço analítico que identifica beneficiários com **3 ou mais faltas consecutivas** (`is_present = false`) na mesma `Activity`, retornando alerta de evasão com `phone_number`.
- **Evolução Incremental (Fase 2):** Gestão de `Event` (seminários/ações pontuais) e portal de autoatendimento para `ROLE_BENEFICIARY`.

## 6. Padrões Obrigatórios de Segurança, Desempenho e Acessibilidade
- **RNF01 (Auth & RBAC):** JWT *stateless* (`Authorization: Bearer <token>`, expiração 8h), senhas com `BCrypt` (fator 10), bloqueio temporário após 5 tentativas falhas no login e `@PreAuthorize` nos endpoints.
- **RNF02 (OWASP Top 10):** Consultas parametrizadas via JPA (anti-SQLi), sanitização contra Stored XSS (uso estrito de `textContent` no TypeScript, proibindo `innerHTML` com dados dinâmicos) e respostas de erro via `ProblemDetail` sem expor *stack traces*.
- **RNF03 (Desempenho):** Payloads JSON enxutos e índices compostos em `(activity_id, beneficiary_id)` para carregamento $< 3,0\text{s}$ em conexões móveis lentas.
- **Acessibilidade (WCAG 2.1 AA):** Layout mobile-first (`360px`), alvos de toque $\ge 48 \times 48\text{px}$ com espaçamento $\ge 8\text{px}$, contraste $\ge 4.5:1$ e **alertas multimodais** (Cor + Ícone/Borda + Texto explícito: `Presente`, `Ausente`, `Risco de Evasão`).

## 7. Convenções de Código e Git
- Commits semânticos em inglês (`feat:`, `fix:`, `chore:`, `docs:`, `test:`).
- Segredos e credenciais via variáveis de ambiente (`${ENV_VAR}`).
- Front-end TypeScript com `strict: true` no `tsconfig.json` e interfaces espelhando os DTOs do back-end.
- Serviços do back-end cobertos por testes unitários com JUnit 5 e Mockito.
