package com.agentplatform.domain.agent.dto;

import com.agentplatform.domain.agent.Agent;

import java.time.Instant;
import java.util.UUID;

public record AgentResponse(
        UUID id,
        String name,
        String purpose,
        String basicInstructions,
        int documentCount,
        Instant createdAt,
        Instant updatedAt
) {
    public static AgentResponse from(Agent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getName(),
                agent.getPurpose(),
                agent.getBasicInstructions(),
                agent.getDocuments().size(),
                agent.getCreatedAt(),
                agent.getUpdatedAt()
        );
    }
}
