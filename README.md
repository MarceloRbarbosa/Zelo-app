# Zelo [![CI](https://github.com/MarceloRbarbosa/Zelo-app/actions/workflows/ci.yml/badge.svg)](https://github.com/MarceloRbarbosa/Zelo-app/actions/workflows/ci.yml)

Aplicação web (PWA) para organizações do Terceiro Setor que troca o controle em papel por um fluxo digital: cadastro de beneficiários, chamada diária nas atividades e alerta quando um beneficiário acumula faltas e corre risco de evasão (**ODS 4 — Educação de Qualidade**).

Projeto piloto com a ONG **Lar de Margarida** (Tamoios, Cabo Frio - RJ).

## Funcionalidades

- [x] **RF01 — Beneficiários:** CPF obrigatório e único, inativação com reativação, documentos anexados e remoção de dados a pedido (LGPD)
- [x] **RF02 — Chamada digital:** registro e correção da chamada do dia, só para alunos matriculados
- [x] **RF03 — Alerta de evasão:** beneficiários com 3 ou mais faltas consecutivas, com telefone para busca ativa
- [x] **RF04 — Equipe e acesso:** senha provisória, troca no primeiro login, perfil, bloqueio imediato e hierarquia de perfis
- [x] **RF05 — Pesquisa de beneficiários:** por nome (sem diferenciar acentos), documento e turma
- [x] **RF06 — Turmas:** horários, responsáveis, matrículas e finalização
- [x] **RF07 — Diário de classe:** grade de presenças por aula, frequência, risco de evasão e impressão
- [x] **RF08 — Eventos:** responsáveis, inscrição e registro de quem participou
- [x] **RF09 — Agenda:** aulas e eventos da ONG, em visão mensal ou semanal
- [x] **RF10 — Assistência:** itens como cesta básica, recebimentos, entregas com busca, saldo e quem registrou (área restrita)
- [x] **RF11 — Início:** risco de evasão, frequência do mês, chamadas pendentes, próximos eventos e assistência

## Stack

- **API (`api/`):** Java 25, Spring Boot 4.1, PostgreSQL 16 e Flyway
- **Web (`web/`):** TypeScript e Vite

## Como Executar

**Pré-requisitos:** Java 25, Node.js 24 e Docker.

```bash
# API — http://localhost:8080
docker compose up -d
cd api
export JWT_SECRET=$(openssl rand -base64 32)
export ADMIN_EMAIL=admin@zelo.local ADMIN_PASSWORD='troque-esta-senha'
./mvnw spring-boot:run
```

O primeiro administrador é criado a partir de `ADMIN_EMAIL` e `ADMIN_PASSWORD`. Detalhes de login e perfis em [docs/api.md](docs/api.md).

Testes unitários e de integração: `cd api && ./mvnw verify` (requer Docker). O relatório de cobertura fica em `api/target/site/jacoco/index.html`.

```bash
# Web — http://localhost:5173
cd web
npm install
npm run dev
```

Dados fictícios para demonstração (equipe, turmas com matrículas, histórico de chamadas, evento e assistência), com a API rodando e o banco vazio: `ADMIN_EMAIL=... ADMIN_PASSWORD=... scripts/seed-demo.sh` (requer `curl` e `jq`).

## Documentação

- [API](docs/api.md): endpoints, validações e configuração do banco
- [Requisitos](docs/requisitos.md): funcionalidades, modelo de dados e permissões
- [CLAUDE.md](CLAUDE.md): arquitetura e regras de desenvolvimento
