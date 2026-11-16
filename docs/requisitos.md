# Requisitos do Zelo

Especificação funcional da plataforma. Regras de código, segurança e acessibilidade ficam no
[`CLAUDE.md`](../CLAUDE.md); endpoints, em [`api.md`](api.md).

## Perfis
Quem acessa a plataforma é só a equipe, com **um perfil por pessoa**: Admin, Coordenador ou
Voluntário. Beneficiários não acessam a plataforma.

## Requisitos funcionais
- **RF01 — Beneficiários:** cadastro com campos obrigatórios, documento único e exclusão lógica.
- **RF02 — Chamada:** lançamento em lote da chamada do dia de uma turma, só para alunos com matrícula ativa.
- **RF03 — Alerta de evasão:** beneficiário com **3 ou mais faltas consecutivas** (sequência atual, após a última presença) na mesma turma, com telefone para busca ativa.
- **RF04 — Equipe e acesso:** admin cadastra membros com senha provisória; troca obrigatória no primeiro login; página de perfil com edição; admin bloqueia membros com efeito imediato.
- **RF05 — Pesquisa de beneficiários:** por nome, documento e turma, em ordem alfabética crescente ou decrescente.
- **RF06 — Turmas:**
  - criar com título, descrição, data de início, horários (dias da semana e hora) e responsáveis (um ou mais); ao criar, abre o diário da turma;
  - adicionar alunos buscando entre os beneficiários cadastrados; só entra quem já tem cadastro;
  - editar dados, horários, responsáveis, alunos e o status do aluno na turma (`ACTIVE`/`LEFT`, na tela "Cursando"/"Saiu");
  - o nome do beneficiário não é editado na turma, só no cadastro dele;
  - a saída do aluno grava a data e preserva o histórico de chamadas;
  - finalizar grava a data de fim.
- **RF07 — Diário de classe:**
  - grade de alunos × datas de aula, com as datas geradas pelos horários da turma;
  - percentual de frequência por aluno e selo "Risco de Evasão";
  - observação por aluno em cada aula;
  - versão para impressão.
- **RF08 — Eventos:** responsáveis definidos na criação; inscrição de beneficiários; registro de quem compareceu. Evento não tem chamada.
- **RF09 — Calendário:** aulas (geradas pelos horários das turmas) e eventos.
- **RF10 — Assistência:**
  - catálogo de itens a distribuir (a cesta básica é um exemplo);
  - registro de entregas por beneficiário, sem limite de frequência;
  - registro de recursos recebidos, em área separada, com acesso só de admin e coordenador.

## Modelo de dados
- **`staff_members`:** `id`, `full_name` (150, not null), `email` (100, unique, not null), `password_hash` (BCrypt), `member_type` (`VOLUNTEER`/`PROFESSIONAL`/`COORDINATOR`), `role` (`ROLE_ADMIN`/`ROLE_COORDINATOR`/`ROLE_VOLUNTEER`), `status` (`ACTIVE`/`BLOCKED`), `must_change_password`, `failed_login_attempts`, `locked_until`, `created_at`.
- **`beneficiaries`:** `id`, `full_name` (150, not null), `document_number` (11, unique quando preenchido), `birth_date` (not null), `phone_number` (20, not null), `address` (255), `status` (`ACTIVE`/`INACTIVE`, soft delete), `created_at`.
- **`activities`:** `id`, `title` (100, not null), `description` (255), `status` (`ACTIVE`/`FINISHED`), `start_date` (not null), `end_date` (preenchida ao finalizar).
- **`activity_schedules`:** `activity_id` (FK), `day_of_week`, `start_time`, `end_time`.
- **`activity_staff`:** `activity_id` (FK), `staff_id` (FK).
- **`enrollments`:** `activity_id` (FK), `beneficiary_id` (FK), `status` (`ACTIVE`/`LEFT`), `left_at` (preenchida na saída), `UNIQUE (activity_id, beneficiary_id)`.
- **`attendances`:** `id`, `activity_id` (FK, not null), `beneficiary_id` (FK, not null), `attendance_date` (not null), `is_present` (not null), `notes` (255, sanitizado), `UNIQUE (activity_id, beneficiary_id, attendance_date)`.
- **`events`:** `id`, `title` (150, not null), `description` (text), `event_date` (timestamp, not null), `location` (150), `status` (`SCHEDULED`/`COMPLETED`/`CANCELED`).
- **`event_staff`:** `event_id` (FK), `staff_id` (FK).
- **`event_participants`:** `event_id` (FK), `beneficiary_id` (FK), `registered_at`, `attended`, `UNIQUE (event_id, beneficiary_id)`.
- **`assistance_items`:** `id`, `name` (100, unique, not null), `description` (255), `active`.
- **`assistance_receipts`:** `id`, `item_id` (FK), `quantity` (> 0), `received_at` (date), `donor` (150), `notes` (255), `recorded_by` (FK staff).
- **`assistance_deliveries`:** `id`, `item_id` (FK), `beneficiary_id` (FK), `quantity` (> 0), `delivered_at` (date), `notes` (255), `recorded_by` (FK staff).

## Matriz de permissões
| Ação | Admin | Coordenador | Voluntário responsável | Outro voluntário |
|---|---|---|---|---|
| Cadastrar e bloquear equipe | ✅ | ❌ | ❌ | ❌ |
| Cadastrar e editar beneficiário | ✅ | ✅ | ❌ | ❌ |
| Pesquisar e ver beneficiários | ✅ | ✅ | ✅ | ✅ |
| Criar, editar e finalizar turma; definir responsáveis | ✅ | ✅ | ❌ | ❌ |
| Matricular e mudar status do aluno na turma | ✅ | ✅ | ❌ | ❌ |
| Acessar o diário e fazer a chamada | ✅ | ✅ | ✅ | ❌ |
| Ver alertas de evasão | ✅ | ✅ | ✅ (suas turmas) | ❌ |
| Criar evento e definir responsáveis | ✅ | ✅ | ❌ | ❌ |
| Inscrever beneficiário e marcar presença no evento | ✅ | ✅ | ✅ (seu evento) | ❌ |
| Assistência: itens, recebimentos e entregas | ✅ | ✅ | ❌ | ❌ |
| Ver o calendário e o próprio perfil | ✅ | ✅ | ✅ | ✅ |

## Fora de escopo por enquanto
Permissionamento fino, envio de e-mail, chamada em evento, portal do beneficiário, limite de vagas
por turma, cadastro de família do beneficiário e limite de frequência nas entregas.
