package com.openroof.openroof.repository;

import com.openroof.openroof.model.config.UserSettings;
import com.openroof.openroof.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUser(User user);
}
