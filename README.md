<div align="center">

# Raxa - Backend API

Backend REST do **Raxa** — organize peladas sem depender do caos de grupos de WhatsApp.

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4-6DB33F?style=flat-square&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white)
![JWT](https://img.shields.io/badge/Auth-JWT-000000?style=flat-square&logo=jsonwebtokens&logoColor=white)

</div>

---

## Sobre o projeto

O Raxa resolve um problema real: confirmar presença em peladas pelo WhatsApp é um caos. Confirmações se perdem, ninguém sabe quantas vagas restam, e o organizador precisa contar na mão.

Este repositório contém a API que alimenta o [app mobile](https://github.com/osantosrei/raxa-mobile). Ela define o contrato oficial de dados, autenticação e regras de negócio do MVP.

**Funcionalidades do MVP**

- Cadastro e login com JWT
- Criação de partidas com limite de vagas
- Entrada e saída de participantes
- Controle de vagas com proteção contra race condition
- Convites compartilháveis via link ou código
- Preview público da partida sem autenticação

---

## Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4 |
| Segurança | Spring Security + JWT stateless |
| Persistência | Spring Data JPA + PostgreSQL 16 |
| Migrations | Flyway |
| Documentação | Springdoc OpenAPI / Swagger UI |
| Containerização | Docker Compose |
| Testes | JUnit 5 + Mockito + H2 |

---

## Rodando localmente

### Pré-requisitos

- [Docker](https://docs.docker.com/get-docker/) e Docker Compose

### 1. Clone o repositório

```bash
git clone https://github.com/osantosrei/raxa-api.git
cd raxa-api
```

### 2. Suba os containers

```bash
docker compose up --build
```

A API inicializa o banco, executa as migrations do Flyway e já está pronta.

| Endpoint | URL |
|---|---|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health check | http://localhost:8080/actuator/health |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

---

## Deploy no Render com Supabase

Deploy recomendado para produção do MVP:

- Plataforma: Render Web Service
- Runtime: Docker
- Banco: Supabase PostgreSQL via Connection Pooler em Session mode
- Health check path: `/actuator/health`
- Frontend liberado no CORS: `https://raxa-*.vercel.app`

### Variáveis no Render

| Variável | Valor |
|---|---|
| `PORT` | `10000` |
| `DB_URL` | `jdbc:postgresql://<supabase-session-pooler-host>:<port>/<database>?sslmode=require` |
| `DB_USER` | usuário do Session Pooler do Supabase |
| `DB_PASSWORD` | senha do banco Supabase |
| `JWT_SECRET` | chave longa e aleatória |
| `JWT_EXPIRATION_MS` | `86400000` |
| `FRONTEND_ORIGINS` | `https://raxa-*.vercel.app` |

Use a string do Supabase em **Connection Pooler -> Session mode**. Evite Transaction mode para esta API, porque JPA/Flyway trabalham melhor com conexões persistentes durante startup e transações.

Depois do deploy, valide:

- `https://<render-service>.onrender.com/actuator/health`
- `https://<render-service>.onrender.com/v3/api-docs`
- `https://<render-service>.onrender.com/swagger-ui.html`

### Variáveis de ambiente

| Variável | Padrão local | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/raxa` | URL JDBC do PostgreSQL |
| `DB_USER` | `raxa` | Usuário do banco |
| `DB_PASSWORD` | `raxa` | Senha do banco |
| `JWT_SECRET` | `mudar-em-producao-...` | Chave de assinatura dos tokens |
| `JWT_EXPIRATION_MS` | `86400000` | Expiração do token (24h em ms) |
| `PORT` | `8080` | Porta HTTP da aplicação |
| `FRONTEND_ORIGINS` | `http://localhost:3000,http://localhost:5173,https://raxa-*.vercel.app` | Origens liberadas no CORS |

> Em produção, substitua `JWT_SECRET` por uma chave longa e gerada aleatoriamente.

---

## Testes

```bash
mvn test
```

A suíte cobre três camadas:

- **Unitários** — regras de negócio de partidas, vagas e convites (sem Spring context)
- **Integração** — fluxos de autenticação e perfil com `@SpringBootTest` e banco H2

---

## API

A documentação interativa completa está no **[Swagger UI](http://localhost:8080/swagger-ui.html)**. Abaixo, referência rápida das rotas.

### Autenticação

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | — | Cadastro; retorna JWT |
| `POST` | `/api/v1/auth/login` | — | Login; retorna JWT |

### Usuário

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | ✓ | Perfil do usuário autenticado |
| `PUT` | `/api/v1/users/me` | ✓ | Atualizar nome e telefone |

### Partidas

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/v1/matches` | ✓ | Criar partida |
| `GET` | `/api/v1/matches` | ✓ | Listar partidas do usuário |
| `GET` | `/api/v1/matches/{id}` | ✓ | Detalhar partida |
| `DELETE` | `/api/v1/matches/{id}` | ✓ | Cancelar partida (somente criador) |
| `POST` | `/api/v1/matches/{id}/join` | ✓ | Entrar na partida |
| `DELETE` | `/api/v1/matches/{id}/leave` | ✓ | Sair da partida |
| `GET` | `/api/v1/matches/{id}/players` | ✓ | Listar participantes |

### Convites

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `GET` | `/api/v1/invites/{code}/resolve` | — | Preview público da partida |
| `POST` | `/api/v1/invites/{code}/join` | ✓ | Entrar via código de convite |

### Formato de erro

Todas as respostas de erro seguem o mesmo contrato:

```json
{
  "timestamp": "2026-06-01T19:00:00Z",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Partida sem vagas disponíveis.",
  "path": "/api/v1/matches/abc/join"
}
```

---

## Fluxo do convite

```
POST /auth/register ou /auth/login
  └─ retorna JWT

POST /matches
  └─ cria a partida, adiciona o criador como primeiro participante
  └─ retorna inviteCode (visível apenas para o criador)

GET /invites/{code}/resolve          ← sem autenticação
  └─ retorna preview da partida (título, local, vagas restantes)

POST /invites/{code}/join            ← requer JWT
  └─ confirma a entrada do jogador na partida
```

---

## Decisões técnicas

**Pacotes por domínio, não por camada.**
A estrutura `domain/match`, `domain/user`, `domain/invite` mantém cada contexto coeso.

**Pessimistic locking no controle de vagas.**
`findByIdForUpdate` aplica `SELECT FOR UPDATE` na entrada em partidas, eliminando a possibilidade de dois jogadores ocuparem a mesma última vaga simultaneamente.

**Unique constraint no banco como segunda linha de defesa.**
`match_players(match_id, user_id)` garante que confirmações duplicadas sejam impossíveis mesmo em cenários de falha acima da camada de serviço.

**`inviteCode` com visibilidade controlada.**
O campo só aparece no `MatchResponse` para o criador da partida. O preview público usa um DTO próprio (`InvitePreviewResponse`) para não expor informações desnecessárias.

**`expires_at` preparado para o futuro.**
Convites não expiram no MVP, mas a coluna já existe no schema para suportar expiração configurável na próxima iteração sem migration destrutiva.

---

## Estrutura do projeto

```
src/main/java/com/raxa
├── config/          # SecurityConfig, OpenApiConfig
├── domain/
│   ├── invite/      # Invite, InviteService, InviteController
│   ├── match/       # Match, MatchService, MatchController
│   ├── player/      # MatchPlayer
│   └── user/        # User, UserService, AuthController, UserController
├── dto/             # Records de request e response
├── exception/       # GlobalExceptionHandler, BusinessException
└── security/        # JwtFilter, JwtService
```

---

## Testando rotas protegidas no Swagger

1. Acesse o [Swagger UI](http://localhost:8080/swagger-ui.html)
2. Execute `POST /api/v1/auth/register` ou `/login`
3. Copie o valor do campo `token`
4. Clique em **Authorize** (cadeado no topo da página)
5. Cole apenas o token — o prefixo `Bearer` é adicionado automaticamente

---

<div align="center">
  <sub>raxa-api · Java 21 · Spring Boot · PostgreSQL · MVP</sub>
</div>
