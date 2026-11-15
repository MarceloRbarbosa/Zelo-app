# Zelo — Gestão Operacional e Combate à Evasão Socioeducativa

## Visão Geral

O **Zelo** é uma aplicação web (PWA) para organizações do Terceiro Setor que troca o controle em papel por um fluxo digital: cadastro de beneficiários, chamada diária nas atividades e alerta quando um beneficiário acumula faltas e corre risco de evasão (**ODS 4 — Educação de Qualidade**).

> **Contexto Piloto:** Validado inicialmente junto à ONG **Lar de Margarida** (Tamoios, Cabo Frio - RJ) para atender rotinas presenciais de oficinas socioeducativas em dispositivos móveis.

---

## Escopo Funcional (MVP)

- **RF01 — Gestão de Beneficiários (`Beneficiary`):** Cadastro e consulta unificada de assistidos com validação de obrigatoriedade, unicidade documental e exclusão lógica (*Soft Delete*).
- **RF02 — Chamada Digital em Lote (`Attendance`):** Registro rápido de frequência diária por atividade recorrente (`Activity`), com restrição composta contra duplicidade de lançamento.
- **RF03 — Alertas de Risco de Evasão (ODS 4):** Motor analítico que identifica beneficiários com **3 ou mais faltas consecutivas**, exibindo alertas multimodais e contato direto para busca ativa.

---

## Status

- [x] Estrutura do monorepo (`api` + `web`)
- [x] Persistência (JPA + Flyway + PostgreSQL)
- [ ] RF01 — Gestão de Beneficiários
- [ ] RF02 — Chamada Digital em Lote
- [ ] RF03 — Alertas de Risco de Evasão
- [ ] Autenticação e RBAC

---

## Arquitetura e Stack Tecnológica

O projeto adota uma arquitetura de **Monorepo** com **Monólito Modular (Package-by-Feature)** no back-end e front-end desacoplado via API RESTful:

### Back-end (`/api`)
- **Linguagem e Framework:** Java 25 & Spring Boot 4.1.2
- **Observabilidade & Validação:** Spring Boot Actuator e Bean Validation
- **Persistência:** Spring Data JPA (Hibernate), PostgreSQL 16+ (Supabase) e Flyway Migrations
- **Segurança (planejado):** Spring Security (JWT Stateless), hash de senhas com BCrypt e controle de acesso por perfis (`RBAC`)

### Front-end (`/web`)
- **Tecnologias:** TypeScript 7, HTML5 e CSS3 modular com Vite 8
- **Padrões:** PWA leve otimizado para redes móveis instáveis (4G/3G) e conformidade com diretrizes de acessibilidade **WCAG 2.1 AA** (alvos de toque $\ge 48\text{px}$, alto contraste e alertas multimodais).

---

## Como Executar Localmente

**Pré-requisitos:** Java 25 e Node.js 24. O Maven não precisa estar instalado: o projeto usa o Maven Wrapper (`./mvnw`).
Para o back-end também é necessário Docker (banco PostgreSQL local).

### Back-end

```bash
docker compose up -d
cd api
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`. Para conferir: `curl http://localhost:8080/actuator/health` deve responder com `"status":"UP"`.

### Front-end

```bash
cd web
npm install
npm run dev
```

A aplicação abre em `http://localhost:5173`.

---

## Estrutura do Repositório

```text
Zelo-app/
├── api/                                     # Back-end RESTful (Java 25 / Spring Boot 4.1.2)
│   └── src/main/java/br/com/zelo/platform/
│       ├── shared/                          # Configurações globais, segurança JWT e ProblemDetail
│       ├── identity/                        # Autenticação, roles e gestão de equipe (StaffMember)
│       ├── beneficiary/                     # Cadastro e acompanhamento de assistidos (RF01)
│       └── program/                         # Atividades, eventos, chamada e alertas (RF02, RF03)
├── web/                                     # Front-end PWA desacoplado (TypeScript + Vite)
│   └── src/                                 # Código-fonte do front-end
├── CLAUDE.md                                # Diretrizes arquiteturais e regras de domínio
└── README.md                                # Documentação principal do projeto
```
