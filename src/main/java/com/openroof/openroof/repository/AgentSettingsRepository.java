package com.openroof.openroof.repository;

import com.openroof.openroof.model.config.AgentSettings;
import com.openroof.openroof.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentSettingsRepository extends JpaRepository<AgentSettings, Long> {
    Optional<AgentSettings> findByUser(User user);
}
