package com.agentplatform.domain.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record AgentRequest(
        @NotBlank String name,
        @NotBlank String purpose,
        @NotBlank String basicInstructions
) {}
