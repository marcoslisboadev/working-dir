package com.agentplatform.domain.agent;

import com.agentplatform.domain.agent.dto.AgentRequest;
import com.agentplatform.domain.agent.dto.AgentResponse;
import com.agentplatform.domain.agent.dto.DocumentRequest;
import com.agentplatform.domain.agent.dto.DocumentResponse;
import com.agentplatform.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentDocumentRepository documentRepository;

    @Transactional(readOnly = true)
    public List<AgentResponse> listAgents(User owner) {
        return agentRepository.findByOwner(owner).stream()
                .map(AgentResponse::from)
                .toList();
    }

    public AgentResponse createAgent(AgentRequest request, User owner) {
        Agent agent = Agent.builder()
                .name(request.name())
                .purpose(request.purpose())
                .basicInstructions(request.basicInstructions())
                .owner(owner)
                .build();
        return AgentResponse.from(agentRepository.save(agent));
    }

    @Transactional(readOnly = true)
    public AgentResponse getAgent(UUID id, User owner) {
        return agentRepository.findByIdAndOwner(id, owner)
                .map(AgentResponse::from)
                .orElseThrow(() -> new NoSuchElementException("Agent not found"));
    }

    public AgentResponse updateAgent(UUID id, AgentRequest request, User owner) {
        Agent agent = agentRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new NoSuchElementException("Agent not found"));

        agent.setName(request.name());
        agent.setPurpose(request.purpose());
        agent.setBasicInstructions(request.basicInstructions());

        return AgentResponse.from(agentRepository.save(agent));
    }

    public void deleteAgent(UUID id, User owner) {
        Agent agent = agentRepository.findByIdAndOwner(id, owner)
                .orElseThrow(() -> new NoSuchElementException("Agent not found"));
        agentRepository.delete(agent);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> listDocuments(UUID agentId, User owner) {
        Agent agent = agentRepository.findByIdAndOwner(agentId, owner)
                .orElseThrow(() -> new NoSuchElementException("Agent not found"));
        return documentRepository.findByAgent(agent).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    public DocumentResponse addDocument(UUID agentId, DocumentRequest request, User owner) {
        Agent agent = agentRepository.findByIdAndOwner(agentId, owner)
                .orElseThrow(() -> new NoSuchElementException("Agent not found"));

        AgentDocument document = AgentDocument.builder()
                .agent(agent)
                .name(request.name())
                .content(request.content())
                .build();

        return DocumentResponse.from(documentRepository.save(document));
    }

    public void deleteDocument(UUID agentId, UUID documentId, User owner) {
        agentRepository.findByIdAndOwner(agentId, owner)
                .orElseThrow(() -> new NoSuchElementException("Agent not found"));

        AgentDocument document = documentRepository.findByIdAndAgentId(documentId, agentId)
                .orElseThrow(() -> new NoSuchElementException("Document not found"));

        documentRepository.delete(document);
    }
}
