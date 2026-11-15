# Zelo

Aplicação web (PWA) para organizações do Terceiro Setor que troca o controle em papel por um fluxo digital: cadastro de beneficiários, chamada diária nas atividades e alerta quando um beneficiário acumula faltas e corre risco de evasão (**ODS 4 — Educação de Qualidade**).

Projeto piloto com a ONG **Lar de Margarida** (Tamoios, Cabo Frio - RJ).

## Funcionalidades

- [x] **RF01 — Beneficiários:** cadastro com validação, documento único e exclusão lógica (API)
- [ ] **RF02 — Chamada digital:** registro de frequência em lote por atividade
- [ ] **RF03 — Alerta de evasão:** beneficiários com 3 ou mais faltas consecutivas
- [ ] Autenticação e controle de acesso por perfil

## Stack

- **API (`api/`):** Java 25, Spring Boot 4.1, PostgreSQL 16 e Flyway
- **Web (`web/`):** TypeScript e Vite

## Como Executar

**Pré-requisitos:** Java 25, Node.js 24 e Docker.

```bash
# API — http://localhost:8080
docker compose up -d
cd api
./mvnw spring-boot:run
```

```bash
# Web — http://localhost:5173
cd web
npm install
npm run dev
```

## Documentação

- [API](docs/api.md): endpoints, validações e configuração do banco
- [CLAUDE.md](CLAUDE.md): arquitetura, modelo de dados e regras do projeto
