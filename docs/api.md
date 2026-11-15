# API

Base URL local: `http://localhost:8080`. Erros seguem o formato `ProblemDetail` (RFC 9457).

## Autenticação

Todas as rotas, exceto o login e `/actuator/health`, exigem o cabeçalho `Authorization: Bearer <token>`. Sem token ou com token inválido a resposta é `401`; com perfil sem permissão, `403`.

`POST /api/v1/auth/login` com `{ "email": "...", "password": "..." }` devolve `{ "token": "...", "expiresAt": "...", "mustChangePassword": false }`. O token vale 8 horas.

Membros cadastrados pelo admin recebem uma **senha provisória** e começam com `mustChangePassword: true`: até trocarem a senha, só conseguem usar `GET /api/v1/me` e `PUT /api/v1/me/password` (o resto responde `403`).

A cada requisição a API confere a situação do membro: conta **bloqueada** perde o acesso na hora (`401`), mesmo com token ainda válido.

Após 5 senhas erradas seguidas, a conta fica bloqueada por 15 minutos. Toda falha de login responde `401` com a mesma mensagem, para não revelar quais e-mails existem.

| Ação | Admin | Coordenador | Voluntário |
|---|---|---|---|
| Gerenciar equipe (`/api/v1/staff-members`) | ✅ | ❌ | ❌ |
| Ver e editar o próprio perfil (`/api/v1/me`) | ✅ | ✅ | ✅ |
| Cadastrar, editar e inativar beneficiários | ✅ | ✅ | ❌ |
| Criar atividades | ✅ | ✅ | ❌ |
| Listar beneficiários e atividades | ✅ | ✅ | ✅ |
| Lançar chamada e ver alertas de evasão | ✅ | ✅ | ✅ |

## Equipe

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/staff-members` | Lista a equipe | `200` |
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
| `GET` | `/api/v1/beneficiaries` | Lista os beneficiários ativos, por nome | `200` |
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

## Atividades

| Método | Rota | Descrição | Respostas |
|---|---|---|---|
| `GET` | `/api/v1/activities` | Lista as atividades ativas, por título | `200` |
| `POST` | `/api/v1/activities` | Cadastra uma atividade | `201`, `400` |

Corpo do `POST`: `title` (obrigatório, até 100), `description` (até 255), `recurrencePattern` (até 50, ex.: `WEEKLY_MON_WED`) e `startDate` (obrigatório).

## Chamada

`POST /api/v1/attendances/batch` registra a chamada de uma atividade em uma data. O lote é gravado inteiro ou nada é gravado.

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

| Status | Quando |
|---|---|
| `400` | campo inválido, data futura, lista vazia, beneficiário repetido no lote ou beneficiário inexistente/inativo |
| `404` | atividade inexistente |
| `409` | atividade encerrada ou chamada já registrada para o beneficiário na data |

Erros de validação trazem o campo em `errors`, ex.: `{ "errors": { "entries[0].beneficiaryId": "must not be null" } }`.

## Alertas de evasão

`GET /api/v1/dropout-alerts` lista os beneficiários cuja sequência **atual** de faltas em uma atividade chegou a 3 ou mais, ou seja, as faltas registradas depois da última presença. Quem voltou a comparecer sai da lista. Filtro opcional: `?activityId=1`.

Considera apenas beneficiários e atividades ativos. Ordenado pelo número de faltas, do maior para o menor.

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
