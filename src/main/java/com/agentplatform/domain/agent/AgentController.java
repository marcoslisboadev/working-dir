package com.agentplatform.domain.agent;

import com.agentplatform.domain.agent.dto.AgentRequest;
import com.agentplatform.domain.agent.dto.AgentResponse;
import com.agentplatform.domain.agent.dto.DocumentRequest;
import com.agentplatform.domain.agent.dto.DocumentResponse;
import com.agentplatform.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping
    public List<AgentResponse> list(@AuthenticationPrincipal User user) {
        return agentService.listAgents(user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentResponse create(@Valid @RequestBody AgentRequest request,
                                @AuthenticationPrincipal User user) {
        return agentService.createAgent(request, user);
    }

    @GetMapping("/{id}")
    public AgentResponse get(@PathVariable UUID id,
                             @AuthenticationPrincipal User user) {
        return agentService.getAgent(id, user);
    }

    @PutMapping("/{id}")
    public AgentResponse update(@PathVariable UUID id,
                                @Valid @RequestBody AgentRequest request,
                                @AuthenticationPrincipal User user) {
        return agentService.updateAgent(id, request, user);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id,
                       @AuthenticationPrincipal User user) {
        agentService.deleteAgent(id, user);
    }

    @GetMapping("/{id}/documents")
    public List<DocumentResponse> listDocuments(@PathVariable UUID id,
                                                @AuthenticationPrincipal User user) {
        return agentService.listDocuments(id, user);
    }

    @PostMapping("/{id}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse addDocument(@PathVariable UUID id,
                                        @Valid @RequestBody DocumentRequest request,
                                        @AuthenticationPrincipal User user) {
        return agentService.addDocument(id, request, user);
    }

    @DeleteMapping("/{id}/documents/{docId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDocument(@PathVariable UUID id,
                               @PathVariable UUID docId,
                               @AuthenticationPrincipal User user) {
        agentService.deleteDocument(id, docId, user);
    }
}
