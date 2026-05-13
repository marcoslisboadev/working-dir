# Agent Platform — Developer Guide

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
- [Environment Variables](#environment-variables)
- [Database](#database)
- [REST API](#rest-api)
- [Authentication](#authentication)
- [Agent Domain](#agent-domain)
- [pgvector / Embeddings](#pgvector--embeddings)
- [Project Structure](#project-structure)

---

## Overview

Agent Platform is a Spring Boot 4 / Java 25 backend that lets authenticated users create and manage AI agent configurations. Each agent holds a name, a purpose, a set of base instructions, and a collection of reference documents whose content can be indexed as vector embeddings for semantic search.

---

## Architecture

```
HTTP Request
    │
    ▼
JwtAuthenticationFilter          reads Bearer token → sets SecurityContext
    │
    ▼
SecurityFilterChain              /api/auth/** public · everything else JWT-required
    │
    ▼
Controller  →  Service  →  Repository  →  PostgreSQL
                 │
              @Transactional boundaries live here
```

**Packages**

| Package | Responsibility |
|---------|---------------|
| `config` | Spring Security filter chain, `PasswordEncoder`, `AuthenticationManager` |
| `security` | JWT generation/validation, auth filter, `UserDetailsService` |
| `domain.user` | User entity, auth endpoints, register/login logic |
| `domain.agent` | Agent + document entities, CRUD, vector similarity query |
| `exception` | Global exception handler — maps domain exceptions to HTTP status codes |

---

## Getting Started

### Prerequisites

- Java 25
- Maven 3.9+
- PostgreSQL 16 with the **pgvector** extension (`CREATE EXTENSION vector;`)

### Run

```bash
# 1. Set environment variables (see section below) or edit application.yml

# 2. Build
mvn clean package -DskipTests

# 3. Start
java -jar target/agent-platform-0.0.1-SNAPSHOT.jar
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `agentplatform` | Database name |
| `DB_USER` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | *(built-in dev key)* | Base64-encoded HMAC-SHA256 key — **must be changed in production** (≥ 32 decoded bytes) |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime in milliseconds (default: 24 h) |
| `PORT` | `8080` | HTTP listen port |

---

## Database

Migrations are managed by **Flyway** and run automatically on startup.

| Migration | Description |
|-----------|-------------|
| `V1__create_tables.sql` | Enables `pgcrypto` and `vector` extensions; creates `users`, `agents`, `agent_documents` tables and indexes |

### Schema overview

```sql
users (id, name, email, password, role, created_at, updated_at)

agents (id, name, purpose, basic_instructions, owner_id → users, created_at, updated_at)

agent_documents (id, agent_id → agents, name, content, embedding vector(1536), created_at, updated_at)
```

The `embedding` column uses an **IVFFlat cosine index** for fast approximate nearest-neighbour search.

---

## REST API

### Base URL

```
http://localhost:8080
```

### Authentication

All endpoints except `/api/auth/register` and `/api/auth/login` require a JWT in the `Authorization` header:

```
Authorization: Bearer <token>
```

---

## Authentication

### Register

```
POST /api/auth/register
```

**Body**

```json
{
  "name": "Alice",
  "email": "alice@example.com",
  "password": "secret123"
}
```

**Response `201`**

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "name": "Alice",
    "email": "alice@example.com",
    "role": "USER",
    "createdAt": "2026-05-13T00:00:00Z"
  }
}
```

---

### Login

```
POST /api/auth/login
```

**Body**

```json
{
  "email": "alice@example.com",
  "password": "secret123"
}
```

**Response `200`** — same shape as register.

---

### Current user

```
GET /api/auth/me
```

**Response `200`**

```json
{
  "id": "uuid",
  "name": "Alice",
  "email": "alice@example.com",
  "role": "USER",
  "createdAt": "2026-05-13T00:00:00Z"
}
```

---

## Agent Domain

### Create agent

```
POST /api/agents
```

**Body**

```json
{
  "name": "Support Bot",
  "purpose": "Answer customer questions about billing and plans.",
  "basicInstructions": "Be concise. Always greet the user by name. Never reveal internal pricing rules."
}
```

**Response `201`**

```json
{
  "id": "uuid",
  "name": "Support Bot",
  "purpose": "Answer customer questions about billing and plans.",
  "basicInstructions": "Be concise. Always greet the user by name. Never reveal internal pricing rules.",
  "documentCount": 0,
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

### List agents

```
GET /api/agents
```

Returns all agents owned by the authenticated user.

---

### Get agent

```
GET /api/agents/{id}
```

Returns 404 if the agent does not exist **or belongs to another user** (no ownership leakage).

---

### Update agent

```
PUT /api/agents/{id}
```

Body: same shape as create. Full replacement — all fields required.

---

### Delete agent

```
DELETE /api/agents/{id}   →  204 No Content
```

Cascades to all documents.

---

### Add reference document

```
POST /api/agents/{id}/documents
```

**Body**

```json
{
  "name": "Pricing FAQ",
  "content": "Q: What is the Pro plan? A: $29/month, unlimited seats..."
}
```

**Response `201`**

```json
{
  "id": "uuid",
  "name": "Pricing FAQ",
  "content": "Q: What is the Pro plan?...",
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

### List documents

```
GET /api/agents/{id}/documents
```

---

### Delete document

```
DELETE /api/agents/{id}/documents/{docId}   →  204 No Content
```

---

## pgvector / Embeddings

The `agent_documents` table has an `embedding vector(1536)` column that is `NULL` until populated. This allows documents to be created immediately and embedded asynchronously.

### Storing an embedding

Use `AgentDocumentRepository.updateEmbedding(id, vectorString)` where `vectorString` is the pgvector text format:

```java
// vectorString example: "[0.12, -0.34, 0.56, ...]"
documentRepository.updateEmbedding(documentId, vectorString);
```

### Similarity search

`AgentDocumentRepository.findSimilarDocuments(agentId, queryVector, limit)` runs a native cosine-distance query:

```sql
SELECT ... , 1 - (embedding <=> CAST(:queryVector AS vector)) AS similarity
FROM agent_documents
WHERE agent_id = :agentId AND embedding IS NOT NULL
ORDER BY embedding <=> CAST(:queryVector AS vector)
LIMIT :limit
```

### Integration pattern

```
1. User uploads document  →  POST /api/agents/{id}/documents
2. Background job calls embedding model (e.g. OpenAI, Claude)
3. Job calls documentRepository.updateEmbedding(id, "[...]")
4. At query time: embed the user question, call findSimilarDocuments,
   inject top-k chunks into the agent's prompt context
```

---

## Project Structure

```
src/main/java/com/agentplatform/
├── AgentPlatformApplication.java
├── config/
│   └── SecurityConfig.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── CustomUserDetailsService.java
├── domain/
│   ├── user/
│   │   ├── User.java
│   │   ├── Role.java
│   │   ├── UserRepository.java
│   │   ├── UserService.java
│   │   ├── AuthController.java
│   │   └── dto/
│   │       ├── RegisterRequest.java
│   │       ├── LoginRequest.java
│   │       ├── AuthResponse.java
│   │       └── UserResponse.java
│   └── agent/
│       ├── Agent.java
│       ├── AgentDocument.java
│       ├── AgentRepository.java
│       ├── AgentDocumentRepository.java
│       ├── AgentService.java
│       ├── AgentController.java
│       └── dto/
│           ├── AgentRequest.java
│           ├── AgentResponse.java
│           ├── DocumentRequest.java
│           └── DocumentResponse.java
└── exception/
    └── GlobalExceptionHandler.java

src/main/resources/
├── application.yml
└── db/migration/
    └── V1__create_tables.sql
```
