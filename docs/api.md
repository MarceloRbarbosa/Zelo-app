# API

Base URL local: `http://localhost:8080`. Erros seguem o formato `ProblemDetail` (RFC 9457).

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

## Banco de dados

As tabelas são criadas pelo Flyway ao subir a API. Sem variáveis definidas, a API usa o banco do `compose.yaml`:

| Variável | Padrão local |
|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/zelo` |
| `DB_USERNAME` | `zelo` |
| `DB_PASSWORD` | `zelo` |
