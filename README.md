# Agent Platform

A RESTful backend for creating and managing AI agent configurations. Built with Spring Boot 4 and Java 25, it provides multi-tenant agent management, JWT authentication, reference document storage, and semantic search via PostgreSQL's pgvector extension.

## Features

- **JWT Authentication** — stateless, BCrypt-hashed, role-based
- **Agent CRUD** — create, read, update, and delete agent definitions scoped per user
- **Document Management** — attach reference documents to agents
- **Semantic Search** — vector embeddings (1536-dim) stored in PostgreSQL with IVFFlat cosine index
- **Multi-Tenancy** — all queries are filtered by the authenticated owner
- **Schema Migrations** — Flyway-managed, versioned SQL

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0 |
| Security | Spring Security + JJWT 0.12 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL 16 + pgvector |
| Migrations | Flyway |
| Build | Maven 3.9+ |

## Prerequisites

- **Java 25+** — [download](https://adoptium.net/)
- **Maven 3.9+** — [download](https://maven.apache.org/download.cgi)
- **PostgreSQL 16+** with the [pgvector](https://github.com/pgvector/pgvector) and `pgcrypto` extensions

### Enable extensions

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS vector;
```

## Getting Started

### 1. Clone

```bash
git clone https://github.com/marcoslisboadev/working-dir.git
cd working-dir
```

### 2. Configure environment

Copy the variables below into your shell or a `.env` file (`.env` is git-ignored):

```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=agent_platform
DB_USER=postgres
DB_PASSWORD=your_password

# At least 256-bit base64-encoded secret
JWT_SECRET=your_base64_secret_here
JWT_EXPIRATION_MS=86400000   # 24 hours

PORT=8080
```

### 3. Build & run

```bash
mvn spring-boot:run
```

Flyway applies migrations automatically on startup. The API will be available at `http://localhost:8080`.

### Build an executable JAR

```bash
mvn package -DskipTests
java -jar target/agent-platform-0.0.1-SNAPSHOT.jar
```

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `agent_platform` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | *(required)* | Database password |
| `JWT_SECRET` | dev key | Base64-encoded HS256 secret (≥ 256 bits) |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime in milliseconds |
| `PORT` | `8080` | HTTP listen port |

> **Production note:** always set a strong `JWT_SECRET`. The built-in default is for local development only.

## API Reference

All endpoints are prefixed with `/api`. Protected endpoints require an `Authorization: Bearer <token>` header.

### Auth

| Method | Path | Auth | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new user |
| `POST` | `/api/auth/login` | Public | Authenticate and obtain JWT |
| `GET` | `/api/auth/me` | JWT | Return authenticated user info |

#### Register

```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "s3cr3t"
}
```

```json
{
  "token": "<jwt>",
  "user": { "id": "...", "name": "Alice", "email": "alice@example.com" }
}
```

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "alice@example.com",
  "password": "s3cr3t"
}
```

---

### Agents

All agent endpoints require JWT.

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/agents` | List all agents owned by the caller |
| `POST` | `/api/agents` | Create a new agent |
| `GET` | `/api/agents/{id}` | Get a single agent |
| `PUT` | `/api/agents/{id}` | Update an agent |
| `DELETE` | `/api/agents/{id}` | Delete an agent (cascades documents) |

#### Create an agent

```http
POST /api/agents
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Support Bot",
  "purpose": "Answer customer support questions.",
  "basicInstructions": "Be concise and friendly."
}
```

---

### Documents

| Method | Path | Description |
|---|---|---|
| `GET` | `/api/agents/{id}/documents` | List documents for an agent |
| `POST` | `/api/agents/{id}/documents` | Add a document to an agent |
| `DELETE` | `/api/agents/{id}/documents/{docId}` | Remove a document |

#### Add a document

```http
POST /api/agents/{id}/documents
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Refund Policy",
  "content": "Customers may request a refund within 30 days...",
  "embedding": [0.021, -0.013]
}
```

> The `embedding` field is optional. When provided it must be a 1536-dimensional float array. Stored with an IVFFlat cosine index for efficient nearest-neighbour queries.

---

## Database Schema

```
users
  id           UUID PK
  name         VARCHAR
  email        VARCHAR UNIQUE
  password     VARCHAR          (bcrypt)
  role         VARCHAR          (USER)
  created_at   TIMESTAMP
  updated_at   TIMESTAMP

agents
  id                  UUID PK
  name                VARCHAR
  purpose             TEXT
  basic_instructions  TEXT
  owner_id            UUID FK → users.id
  created_at          TIMESTAMP
  updated_at          TIMESTAMP

agent_documents
  id          UUID PK
  agent_id    UUID FK → agents.id  (CASCADE DELETE)
  name        VARCHAR
  content     TEXT
  embedding   vector(1536)          (nullable, IVFFlat cosine index)
  created_at  TIMESTAMP
  updated_at  TIMESTAMP
```

## Architecture

```
HTTP Request
    │
    ▼
JwtAuthenticationFilter          ← validates Bearer token
    │
    ▼
Controller (AuthController / AgentController)
    │
    ▼
Service (UserService / AgentService)   ← business logic, @Transactional
    │
    ├─► Repository (Spring Data JPA)   ← data access, ownership-scoped queries
    │       │
    │       ▼
    │   PostgreSQL 16 + pgvector
    │
    └─► JwtTokenProvider               ← JWT generation & validation
```

**Package layout:**

```
com.agentplatform
├── config/          SecurityConfig
├── security/        JwtTokenProvider, JwtAuthenticationFilter, CustomUserDetailsService
├── domain/
│   ├── user/        User, UserService, AuthController, DTOs
│   └── agent/       Agent, AgentDocument, AgentService, AgentController, DTOs
└── exception/       GlobalExceptionHandler
```

## Running Tests

```bash
mvn test
```

## License

Distributed under the [Apache License 2.0](LICENSE).
