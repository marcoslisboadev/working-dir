CREATE EXTENSION IF NOT EXISTS "pgcrypto";
CREATE EXTENSION IF NOT EXISTS "vector";

-- ─── Users ───────────────────────────────────────────────────────────────────

CREATE TABLE users (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(255)        NOT NULL,
    email        VARCHAR(255)        NOT NULL UNIQUE,
    password     VARCHAR(255)        NOT NULL,
    role         VARCHAR(50)         NOT NULL DEFAULT 'USER',
    created_at   TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

-- ─── Agents ──────────────────────────────────────────────────────────────────

CREATE TABLE agents (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name               VARCHAR(255) NOT NULL,
    purpose            TEXT         NOT NULL,
    basic_instructions TEXT         NOT NULL,
    owner_id           UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agents_owner_id ON agents(owner_id);

-- ─── Agent Documents ─────────────────────────────────────────────────────────

CREATE TABLE agent_documents (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id   UUID         NOT NULL REFERENCES agents(id) ON DELETE CASCADE,
    name       VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    embedding  vector(1536),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_documents_agent_id ON agent_documents(agent_id);

-- IVFFlat index for cosine similarity search (build after populating embeddings)
CREATE INDEX idx_agent_documents_embedding
    ON agent_documents
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
