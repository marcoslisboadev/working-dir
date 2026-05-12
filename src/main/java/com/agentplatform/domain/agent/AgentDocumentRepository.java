package com.agentplatform.domain.agent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentDocumentRepository extends JpaRepository<AgentDocument, UUID> {

    List<AgentDocument> findByAgent(Agent agent);

    Optional<AgentDocument> findByIdAndAgentId(UUID id, UUID agentId);

    @Modifying
    @Query(value = "UPDATE agent_documents SET embedding = CAST(:embedding AS vector) WHERE id = :id",
           nativeQuery = true)
    void updateEmbedding(@Param("id") UUID id, @Param("embedding") String embedding);

    @Query(value = """
            SELECT id, agent_id, name, content, created_at, updated_at,
                   1 - (embedding <=> CAST(:queryVector AS vector)) AS similarity
            FROM agent_documents
            WHERE agent_id = :agentId
              AND embedding IS NOT NULL
            ORDER BY embedding <=> CAST(:queryVector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<Object[]> findSimilarDocuments(
            @Param("agentId") UUID agentId,
            @Param("queryVector") String queryVector,
            @Param("limit") int limit);
}
