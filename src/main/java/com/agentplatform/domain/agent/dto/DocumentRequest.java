package com.agentplatform.domain.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentRequest(
        @NotBlank String name,
        @NotBlank String content
) {}
