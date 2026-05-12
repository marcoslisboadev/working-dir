package com.agentplatform.domain.agent.dto;

import com.agentplatform.domain.agent.AgentDocument;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String name,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
    public static DocumentResponse from(AgentDocument doc) {
        return new DocumentResponse(
                doc.getId(),
                doc.getName(),
                doc.getContent(),
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
