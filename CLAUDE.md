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
- `identity/`: Autenticação (`/api/v1/auth/login`), troca de senha, perfil do usuário e gestão de equipe (`StaffMember`).
- `beneficiary/`: Cadastro, pesquisa e acompanhamento de beneficiários (`Beneficiary`).
- `program/`: Turmas (`Activity`) com horários, responsáveis e matrículas; eventos (`Event`) com responsáveis e participantes; chamada (`Attendance`); alertas de evasão e calendário.

## 4. Modelo de Dados Relacional Evolutivo (Inglês)
O schema evolui de forma incremental por migrations Flyway versionadas.

- **`staff_members`** (Equipe — único público com acesso à plataforma):
  - `id` (BigInt PK), `full_name` (Varchar 150, not null), `email` (Varchar 100, unique, not null), `password_hash` (Varchar 255, BCrypt), `member_type` (`VOLUNTEER`, `PROFESSIONAL`, `COORDINATOR`), `role` (**um perfil por pessoa**: `ROLE_ADMIN`, `ROLE_COORDINATOR`, `ROLE_VOLUNTEER`), `status` (`ACTIVE`/`BLOCKED`), `must_change_password` (Boolean), `failed_login_attempts`, `locked_until`, `created_at` (Timestamp).
- **`beneficiaries`** (Assistidos — **não acessam a plataforma**):
  - `id` (BigInt PK), `full_name` (Varchar 150, not null), `document_number` (Varchar 11, unique quando preenchido), `birth_date` (Date, not null), `phone_number` (Varchar 20, not null — obrigatório para busca ativa), `address` (Varchar 255, nullable), `status` (`ACTIVE`/`INACTIVE` — Soft Delete), `created_at` (Timestamp).
- **`activities`** (Turmas/Oficinas recorrentes):
  - `id` (BigInt PK), `title` (Varchar 100, not null), `description` (Varchar 255, nullable), `status` (`ACTIVE`/`FINISHED`), `start_date` (Date, not null), `end_date` (Date, preenchida ao finalizar a turma).
- **`activity_schedules`** (Horários da turma, base do calendário): `activity_id` (FK), `day_of_week`, `start_time`, `end_time`.
- **`activity_staff`** (Responsáveis pela turma — uma turma pode ter vários): `activity_id` (FK), `staff_id` (FK).
- **`enrollments`** (Matrícula do beneficiário na turma): `activity_id` (FK), `beneficiary_id` (FK), `UNIQUE (activity_id, beneficiary_id)`.
- **`events`** (Ações pontuais / seminários — não recorrentes):
  - `id` (BigInt PK), `title` (Varchar 150, not null), `description` (Text, nullable), `event_date` (Timestamp, not null), `location` (Varchar 150, nullable), `status` (`SCHEDULED`/`COMPLETED`/`CANCELED`).
- **`event_staff`** (Responsáveis pelo evento): `event_id` (FK), `staff_id` (FK).
- **`event_participants`** (Inscrição e participação — evento não tem chamada): `event_id` (FK), `beneficiary_id` (FK), `registered_at`, `attended` (Boolean), `UNIQUE (event_id, beneficiary_id)`.
- **`attendances`** (Chamada das turmas):
  - `id` (BigInt PK), `activity_id` (FK -> `activities`, not null), `beneficiary_id` (FK -> `beneficiaries`, not null), `attendance_date` (Date, not null), `is_present` (Boolean, not null), `notes` (Varchar 255, sanitized).
  - **Restrição Crítica:** `UNIQUE (activity_id, beneficiary_id, attendance_date)`.

## 5. Escopo Funcional Priorizado
- **RF01:** CRUD de `Beneficiary` com validação de campos obrigatórios, unicidade de documento e Soft Delete.
- **RF02:** Lançamento em lote da chamada diária (`POST /api/v1/attendances/batch`) vinculada a uma turma.
- **RF03:** Alerta de evasão para beneficiários com **3 ou mais faltas consecutivas** (sequência atual, após a última presença) na mesma turma, com `phone_number`.
- **RF04 — Equipe e acesso:** admin cadastra membros com **senha provisória** definida por ele; **troca obrigatória no primeiro login**; página de **perfil** com edição; admin **bloqueia** membros com efeito imediato.
- **RF05 — Pesquisa de beneficiários:** por nome, documento e turma, com ordenação alfabética crescente e decrescente.
- **RF06 — Turmas:** cadastro com data de início, horários e responsáveis; **finalizar** registra a data de fim; matrícula de beneficiários.
- **RF07 — Eventos:** responsáveis definidos na criação; inscrição de beneficiários e registro de quem participou.
- **RF08 — Calendário:** todas as aulas (geradas pelos horários das turmas) e eventos da ONG.

### Matriz de permissões
| Ação | Admin | Coordenador | Voluntário responsável | Outro voluntário |
|---|---|---|---|---|
| Cadastrar e bloquear equipe | ✅ | ❌ | ❌ | ❌ |
| Cadastrar beneficiário | ✅ | ✅ | ❌ | ❌ |
| Pesquisar e ver beneficiários | ✅ | ✅ | ✅ | ✅ |
| Criar/finalizar turma e definir responsáveis | ✅ | ✅ | ❌ | ❌ |
| Matricular na turma | ✅ | ✅ | ❌ | ❌ |
| Acessar a turma e fazer a chamada | ✅ | ✅ | ✅ | ❌ |
| Ver alertas de evasão | ✅ | ✅ | ✅ (suas turmas) | ❌ |
| Criar evento e definir responsáveis | ✅ | ✅ | ❌ | ❌ |
| Inscrever beneficiário e marcar participação | ✅ | ✅ | ✅ (seu evento) | ❌ |
| Ver o calendário e o próprio perfil | ✅ | ✅ | ✅ | ✅ |

## 6. Padrões Obrigatórios de Segurança, Desempenho e Acessibilidade
- **RNF01 (Auth & RBAC):** JWT *stateless* (`Authorization: Bearer <token>`, expiração 8h), senhas com `BCrypt` (fator 10), bloqueio temporário após 5 tentativas falhas no login, `@PreAuthorize` nos endpoints e verificação do status do membro a cada requisição (bloqueio vale na hora, sem esperar o token expirar).
- **RNF02 (OWASP Top 10):** Consultas parametrizadas via JPA (anti-SQLi), sanitização contra Stored XSS (uso estrito de `textContent` no TypeScript, proibindo `innerHTML` com dados dinâmicos) e respostas de erro via `ProblemDetail` sem expor *stack traces*.
- **RNF03 (Desempenho):** Payloads JSON enxutos e índices compostos em `(activity_id, beneficiary_id)` para carregamento $< 3,0\text{s}$ em conexões móveis lentas.
- **Acessibilidade (WCAG 2.1 AA):** Layout mobile-first (`360px`), alvos de toque $\ge 48 \times 48\text{px}$ com espaçamento $\ge 8\text{px}$, contraste $\ge 4.5:1$ e **alertas multimodais** (Cor + Ícone/Borda + Texto explícito: `Presente`, `Ausente`, `Risco de Evasão`).

## 7. Convenções de Código e Git
- Commits semânticos em inglês (`feat:`, `fix:`, `chore:`, `docs:`, `test:`).
- Segredos e credenciais via variáveis de ambiente (`${ENV_VAR}`).
- Front-end TypeScript com `strict: true` no `tsconfig.json` e interfaces espelhando os DTOs do back-end.
- Serviços do back-end cobertos por testes unitários com JUnit 5 e Mockito.
