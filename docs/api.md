# API

Base URL local: `http://localhost:8080`. Erros seguem o formato `ProblemDetail` (RFC 9457), inclusive os `401` e `403` de autenticação e permissão.

## Autenticação

Todas as rotas, exceto o login e `/actuator/health`, exigem o cabeçalho `Authorization: Bearer <token>`. Sem token ou com token inválido a resposta é `401`; com perfil sem permissão, `403`.

`POST /api/v1/auth/login` com `{ "email": "...", "password": "..." }` devolve `{ "token": "...", "expiresAt": "...", "mustChangePassword": false }`. O token vale 8 horas.

Membros cadastrados pelo admin recebem uma **senha provisória** e começam com `mustChangePassword: true`: até trocarem a senha, só conseguem usar `GET /api/v1/me` e `PUT /api/v1/me/password` (o resto responde `403`).

A cada requisição a API confere a situação do membro: conta **bloqueada** perde o acesso na hora (`401`), mesmo com token ainda válido.

Após 5 senhas erradas seguidas, a conta fica bloqueada por 15 minutos. Toda falha de login responde `401` com a mesma mensagem, para não revelar quais e-mails existem.

| Ação | Admin | Coordenador | Voluntário |
|---|---|---|---|
| Gerenciar equipe (`/api/v1/staff-members`) | ✅ | ❌ | ❌ |
| Listar a equipe (para escolher responsáveis) | ✅ | ✅ | ❌ |
| Ver e editar o próprio perfil (`/api/v1/me`) | ✅ | ✅ | ✅ |
| Cadastrar, editar e inativar beneficiários | ✅ | ✅ | ❌ |
| Criar, editar e finalizar turmas; matricular | ✅ | ✅ | ❌ |
| Pesquisar beneficiários | ✅ | ✅ | ✅ |
| Ver turmas, diário, chamada e alertas | ✅ todas | ✅ todas | só das turmas em que é responsável (`403` nas outras) |
| Ver eventos | ✅ | ✅ | ✅ |
| Criar, editar, concluir e cancelar eventos | ✅ | ✅ | ❌ |
| Inscrever beneficiários e marcar presença no evento | ✅ | ✅ | só nos eventos em que é responsável |
| Assistência: itens, recebimentos e entregas | ✅ | ✅ | ❌ |

## Equipe

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/staff-members` | Lista a equipe (admin e coordenador) | `200` |
| `POST` | `/api/v1/staff-members` | Cadastra um membro com senha provisória | `201`, `400`, `409` |
| `POST` | `/api/v1/staff-members/{id}/block` | Bloqueia o acesso (o admin não bloqueia a si mesmo) | `200`, `400`, `404` |
| `POST` | `/api/v1/staff-members/{id}/unblock` | Desbloqueia | `200`, `404` |

Corpo do `POST`: `fullName`, `email` (único), `password` (senha provisória, 8 a 72 caracteres), `memberType` (`VOLUNTEER`, `PROFESSIONAL`, `COORDINATOR`) e `role` (`ROLE_ADMIN`, `ROLE_COORDINATOR`, `ROLE_VOLUNTEER`).

## Perfil

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/me` | Dados do membro logado | `200` |
| `PUT` | `/api/v1/me` | Altera o nome (`{ "fullName": "..." }`) | `200`, `400` |
| `PUT` | `/api/v1/me/password` | Troca a senha (`{ "currentPassword": "...", "newPassword": "..." }`) | `204`, `400` |

## Beneficiários

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/beneficiaries` | Pesquisa os beneficiários ativos | `200`, `400` |
| `GET` | `/api/v1/beneficiaries/{id}` | Busca um beneficiário ativo | `200`, `404` |
| `POST` | `/api/v1/beneficiaries` | Cadastra um beneficiário | `201`, `400`, `409` |
| `PUT` | `/api/v1/beneficiaries/{id}` | Atualiza um beneficiário ativo | `200`, `400`, `404`, `409` |
| `DELETE` | `/api/v1/beneficiaries/{id}` | Inativa o beneficiário (*soft delete*) | `204`, `404` |

Corpo de `POST` e `PUT`:

```json
{
  "fullName": "Ana Souza",
  "documentNumber": "12345678901",
  "birthDate": "2012-03-10",
  "phoneNumber": "22999990000",
  "address": "Rua A, 10"
}
```

| Campo | Regra |
|---|---|
| `fullName` | obrigatório, até 150 caracteres |
| `documentNumber` | opcional, 11 dígitos, único |
| `birthDate` | obrigatório, data no passado |
| `phoneNumber` | obrigatório, até 20 caracteres |
| `address` | opcional, até 255 caracteres |

Erros: `400` dados inválidos, `404` beneficiário inexistente ou inativo, `409` documento já cadastrado.

Filtros da pesquisa, todos opcionais e combináveis: `name` (parte do nome, sem diferenciar maiúsculas nem acentos), `documentNumber` (exato), `activityId` (matriculados ativos na turma) e `order` (`ASC` ou `DESC`, padrão `ASC`, pelo nome). Ex.: `GET /api/v1/beneficiaries?name=souza&order=DESC`.

## Turmas

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/activities` | Lista as turmas por título; `?status=FINISHED` para as finalizadas (padrão `ACTIVE`) | `200` |
| `GET` | `/api/v1/activities/{id}` | Busca uma turma | `200`, `403`, `404` |
| `POST` | `/api/v1/activities` | Cadastra uma turma | `201`, `400` |
| `PUT` | `/api/v1/activities/{id}` | Edita dados, horários e responsáveis | `200`, `400`, `404`, `409` |
| `POST` | `/api/v1/activities/{id}/finish` | Finaliza (`{ "endDate": "2026-12-18" }`) | `200`, `400`, `404`, `409` |

Corpo de `POST` e `PUT`:

```json
{
  "title": "Capoeira infantil",
  "description": "Turma da tarde",
  "startDate": "2026-11-02",
  "schedules": [{ "dayOfWeek": "MONDAY", "startTime": "14:00", "endTime": "15:30" }],
  "staffIds": [2, 3]
}
```

`title` obrigatório (até 100), `description` até 255, `startDate` obrigatório, ao menos um horário (`dayOfWeek` de `MONDAY` a `SUNDAY`, fim depois do início, sem repetir dia e hora) e ao menos um responsável existente. A resposta traz `endDate`, `schedules` e `staff` (`id` e `fullName`). Turma finalizada não é editada (`409`).

### Matrículas

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/activities/{id}/enrollments` | Alunos da turma, por nome | `200`, `403`, `404` |
| `POST` | `/api/v1/activities/{id}/enrollments` | Matricula (`{ "beneficiaryId": 7 }`) | `201`, `404`, `409` |
| `PUT` | `/api/v1/activities/{id}/enrollments/{beneficiaryId}` | Muda o status (`{ "status": "LEFT", "leftAt": "2026-11-20" }` ou `{ "status": "ACTIVE" }`) | `200`, `400`, `404`, `409` |

Status do aluno: `ACTIVE` (cursando) ou `LEFT` (saiu, com `leftAt`). A saída preserva o histórico de chamadas; matricular de novo quem saiu reativa a mesma matrícula. Só beneficiários ativos são matriculados.

### Diário

`GET /api/v1/activities/{id}/diary?from=2026-11-01&to=2026-11-30` (período de até 366 dias):

```json
{
  "activityId": 1,
  "title": "Capoeira infantil",
  "status": "ACTIVE",
  "dates": ["2026-11-02", "2026-11-09"],
  "students": [
    {
      "beneficiaryId": 7,
      "fullName": "Ana Souza",
      "enrollmentStatus": "ACTIVE",
      "leftAt": null,
      "attendanceRate": 75,
      "dropoutRisk": false,
      "marks": [{ "date": "2026-11-02", "present": true, "notes": null }]
    }
  ]
}
```

`dates` são os dias de aula pelos horários, entre o início e o fim da turma, mais qualquer data que já tenha chamada. `attendanceRate` é o percentual de presenças em todas as chamadas do aluno na turma (`null` sem chamadas). `dropoutRisk` segue a regra dos alertas de evasão.

### Aulas do calendário

`GET /api/v1/class-sessions?from=2026-11-01&to=2026-11-30` (até 366 dias, qualquer perfil) lista cada aula do período gerada pelos horários das turmas, dentro do início e fim de cada uma, ordenada por data e hora: `activityId`, `title`, `status`, `date`, `startTime`, `endTime`. O calendário junta essas aulas com os eventos de `GET /api/v1/events`.

## Chamada

`POST /api/v1/attendances/batch` registra a chamada de uma turma em uma data. Só entram alunos com matrícula ativa. O lote é gravado inteiro ou nada é gravado.

```json
{
  "activityId": 1,
  "attendanceDate": "2026-11-10",
  "entries": [
    { "beneficiaryId": 1, "present": true },
    { "beneficiaryId": 2, "present": false, "notes": "Doente" }
  ]
}
```

Resposta `201`: `{ "activityId": 1, "attendanceDate": "2026-11-10", "registered": 2, "present": 1, "absent": 1 }`.

`PUT /api/v1/attendances/batch`, com o mesmo corpo, **corrige** a chamada da data: altera presença e observação de quem já tem registro e registra quem ainda não tem. Responde `200` com o mesmo formato e tem as mesmas validações, exceto o `409` de chamada já registrada.

| Status | Quando |
|---|---|
| `400` | campo inválido, data futura, lista vazia, beneficiário repetido no lote, inexistente/inativo ou sem matrícula ativa na turma |
| `403` | voluntário que não é responsável pela turma |
| `404` | turma inexistente |
| `409` | turma finalizada ou chamada já registrada para o beneficiário na data |

Erros de validação trazem o campo em `errors`, ex.: `{ "errors": { "entries[0].beneficiaryId": "must not be null" } }`.

## Eventos

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/events?from=2026-12-01T00:00:00Z&to=2026-12-31T23:59:59Z` | Eventos do período (até 366 dias), por data | `200`, `400` |
| `GET` | `/api/v1/events/{id}` | Busca um evento | `200`, `404` |
| `POST` | `/api/v1/events` | Cadastra um evento | `201`, `400` |
| `PUT` | `/api/v1/events/{id}` | Edita um evento agendado | `200`, `400`, `404`, `409` |
| `POST` | `/api/v1/events/{id}/complete` | Marca como concluído | `200`, `404`, `409` |
| `POST` | `/api/v1/events/{id}/cancel` | Cancela | `200`, `404`, `409` |

Corpo de `POST` e `PUT`: `title` (obrigatório, até 150), `description`, `eventDate` (obrigatório, data e hora com fuso, ex.: `2026-12-05T10:00:00-03:00`), `location` (até 150) e `staffIds` (ao menos um responsável existente). Status: `SCHEDULED`, `COMPLETED` ou `CANCELED`; só evento agendado é editado, concluído ou cancelado (`409` nos outros casos). Evento não tem chamada.

### Participantes

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/events/{id}/participants` | Inscritos, por nome | `200`, `403`, `404` |
| `POST` | `/api/v1/events/{id}/participants` | Inscreve (`{ "beneficiaryId": 7 }`) | `201`, `403`, `404`, `409` |
| `PUT` | `/api/v1/events/{id}/participants/{beneficiaryId}` | Marca presença (`{ "attended": true }`) | `200`, `403`, `404`, `409` |
| `DELETE` | `/api/v1/events/{id}/participants/{beneficiaryId}` | Cancela a inscrição | `204`, `403`, `404`, `409` |

Resposta: `beneficiaryId`, `fullName`, `registeredAt` e `attended`. Evento cancelado não aceita mudanças (`409`).

## Assistência

Área restrita a admin e coordenador.

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/assistance/items` | Itens por nome, com `received`, `delivered` e `balance` (recebido menos entregue) | `200` |
| `POST` | `/api/v1/assistance/items` | Cadastra um item (`{ "name": "Cesta básica", "description": "...", "active": true }`) | `201`, `400`, `409` |
| `PUT` | `/api/v1/assistance/items/{id}` | Edita ou inativa um item | `200`, `400`, `404`, `409` |
| `GET` | `/api/v1/assistance/receipts?from=2026-11-01&to=2026-11-30` | Recursos recebidos no período (até 366 dias), mais recentes primeiro | `200`, `400` |
| `POST` | `/api/v1/assistance/receipts` | Registra um recebimento | `201`, `400`, `404`, `409` |
| `GET` | `/api/v1/assistance/deliveries?from=...&to=...` ou `?beneficiaryId=7` | Entregas do período ou histórico do beneficiário | `200`, `400` |
| `POST` | `/api/v1/assistance/deliveries` | Registra uma entrega a um beneficiário ativo | `201`, `400`, `404`, `409` |

Recebimento: `itemId`, `quantity` (maior que zero), `receivedAt` (até hoje), `donor` (até 150) e `notes` (até 255). Entrega: `itemId`, `beneficiaryId`, `quantity`, `deliveredAt` e `notes`. Quem registrou fica gravado. Nome de item é único sem diferenciar maiúsculas (`409`); item inativo não recebe movimentos (`409`). Não há limite de frequência nem bloqueio por saldo: o saldo é informativo.

## Alertas de evasão

`GET /api/v1/dropout-alerts` lista os beneficiários cuja sequência **atual** de faltas em uma turma chegou a 3 ou mais, ou seja, as faltas registradas depois da última presença. Quem voltou a comparecer sai da lista. Filtro opcional: `?activityId=1`.

Considera apenas beneficiários ativos, com matrícula ativa, em turmas ativas. O voluntário vê só as turmas em que é responsável. Ordenado pelo número de faltas, do maior para o menor.

```json
[
  {
    "activityId": 1,
    "activityTitle": "Oficina de leitura",
    "beneficiaryId": 7,
    "beneficiaryName": "Ana Souza",
    "phoneNumber": "22999990000",
    "consecutiveAbsences": 4,
    "lastAttendanceDate": "2026-11-05"
  }
]
```

## Banco de dados

As tabelas são criadas pelo Flyway ao subir a API. Sem variáveis definidas, a API usa o banco do `compose.yaml`:

| Variável | Padrão local |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/zelo` |
| `DB_USERNAME` | `zelo` |
| `DB_PASSWORD` | `zelo` |

## Segurança

| Variável | Obrigatória | Uso |
|---|---|---|
| `JWT_SECRET` | sim | chave de assinatura dos tokens, com pelo menos 32 bytes; sem ela a API não sobe |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | não | criam o primeiro administrador se ainda não existir nenhum |
