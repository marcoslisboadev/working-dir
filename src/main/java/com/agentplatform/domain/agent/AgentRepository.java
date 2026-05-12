package com.agentplatform.domain.agent;

import com.agentplatform.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgentRepository extends JpaRepository<Agent, UUID> {

    List<Agent> findByOwner(User owner);

    Optional<Agent> findByIdAndOwner(UUID id, User owner);
}
