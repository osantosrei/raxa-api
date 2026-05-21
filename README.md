# raxa-api

Backend REST da aplicacao Raxa, um MVP para organizacao de partidas de futebol amador. A API e a fonte oficial de dados e contrato para o app mobile.

## Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Security com JWT
- Spring Data JPA
- PostgreSQL
- Flyway
- Docker Compose
- Springdoc OpenAPI / Swagger UI
- JUnit, Mockito e H2 para testes

## Como rodar com Docker

```powershell
docker compose up --build
```

A API ficara disponivel em:

- API: http://localhost:8080
- Health check: http://localhost:8080/actuator/health
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Variaveis de ambiente

| Variavel | Padrao local | Descricao |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/raxa` | URL JDBC do PostgreSQL |
| `DB_USER` | `raxa` | Usuario do banco |
| `DB_PASSWORD` | `raxa` | Senha do banco |
| `JWT_SECRET` | `mudar-em-producao-segredo-longo-e-aleatorio` | Chave usada para assinar tokens |
| `JWT_EXPIRATION_MS` | `86400000` | Tempo de expiracao do token em ms |

Em producao, substitua obrigatoriamente `JWT_SECRET` por uma chave longa e aleatoria.

## Rodando testes

No Windows, se o Maven Wrapper local nao estiver disponivel, use o Maven instalado/cacheado no ambiente:

```powershell
mvn test
```

A suite cobre:

- regras de negocio de usuarios, partidas e convites;
- controle de vagas e status da partida;
- fluxo de autenticacao e perfil com contexto Spring e banco H2 em memoria.

## Principais rotas

### Auth

| Metodo | Rota | Auth | Descricao |
|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Nao | Cadastro com retorno de JWT |
| `POST` | `/api/v1/auth/login` | Nao | Login com retorno de JWT |

### Users

| Metodo | Rota | Auth | Descricao |
|---|---|---|---|
| `GET` | `/api/v1/users/me` | Sim | Perfil do usuario autenticado |
| `PUT` | `/api/v1/users/me` | Sim | Atualizar nome e telefone |

### Matches

| Metodo | Rota | Auth | Descricao |
|---|---|---|---|
| `POST` | `/api/v1/matches` | Sim | Criar partida e gerar convite |
| `GET` | `/api/v1/matches` | Sim | Listar partidas do usuario |
| `GET` | `/api/v1/matches/{id}` | Sim | Detalhar partida |
| `DELETE` | `/api/v1/matches/{id}` | Sim | Cancelar partida |
| `POST` | `/api/v1/matches/{id}/join` | Sim | Entrar diretamente na partida |
| `DELETE` | `/api/v1/matches/{id}/leave` | Sim | Sair da partida |
| `GET` | `/api/v1/matches/{id}/players` | Sim | Listar participantes |

### Invites

| Metodo | Rota | Auth | Descricao |
|---|---|---|---|
| `GET` | `/api/v1/invites/{code}/resolve` | Nao | Preview publico da partida |
| `POST` | `/api/v1/invites/{code}/join` | Sim | Entrar via codigo de convite |

## Fluxo basico

1. Usuario cria conta em `/api/v1/auth/register`.
2. App mobile armazena o JWT retornado.
3. Usuario cria partida em `/api/v1/matches`.
4. API cria o participante inicial e retorna `inviteCode` para o criador.
5. Destinatario resolve `/api/v1/invites/{code}/resolve` sem login.
6. Depois de autenticar, destinatario confirma entrada em `/api/v1/invites/{code}/join`.

## Decisoes tecnicas

- Pacotes organizados por dominio (`user`, `match`, `player`, `invite`) para manter fronteiras claras.
- JWT stateless; rotas protegidas exigem `Authorization: Bearer <token>`.
- Flyway versiona o schema do PostgreSQL.
- `MatchRepository.findByIdForUpdate` aplica pessimistic locking na entrada em partidas para evitar overbooking.
- `match_players` tem unique constraint em `(match_id, user_id)` para impedir confirmacao duplicada no banco.
- `inviteCode` so aparece para o criador em respostas de partida; o preview publico usa DTO proprio.
- Convites nao expiram no MVP, mas `expires_at` ja existe para evolucao futura.
- Respostas de erro sao padronizadas por `GlobalExceptionHandler`.

## Swagger e JWT

O Swagger UI expoe o esquema `bearerAuth`. Para testar rotas protegidas:

1. Cadastre ou autentique um usuario.
2. Copie o valor de `token`.
3. Clique em `Authorize` no Swagger.
4. Informe apenas o token JWT no campo Bearer.

## Estrutura

```text
src/main/java/com/raxa
|-- config
|-- domain
|   |-- invite
|   |-- match
|   |-- player
|   `-- user
|-- dto
|-- exception
`-- security
```

## Status do MVP

- Cadastro e login com JWT
- Perfil basico
- CRUD principal de partidas
- Entrada e saida de participantes
- Controle de vagas
- Convites compartilhaveis
- Swagger com JWT
- Testes unitarios e integracao com H2
