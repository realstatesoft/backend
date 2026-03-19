package com.openroof.openroof.repository.Auth;

import com.openroof.openroof.model.user.User;
import com.openroof.openroof.model.user.UserSession;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Auth: Enrique Rios
 * Desc: Repositorio para gestionar la persistencia y validación de sesiones de
 * usuario.
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM UserSession s WHERE s.tokenHash = :tokenHash")
    Optional<UserSession> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    boolean existsByTokenHash(String tokenHash);

    // Permite eliminar sesiones por token (Logout)
    void deleteByTokenHash(String tokenHash);

    // Permite cerrar todas las sesiones de un usuario (Logout global)
    void deleteByUser(User user);
}
