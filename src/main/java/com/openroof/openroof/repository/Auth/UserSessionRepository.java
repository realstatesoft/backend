package com.openroof.openroof.repository.Auth;

import com.openroof.openroof.model.user.User;
import com.openroof.openroof.model.user.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
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


    boolean existsByTokenHash(String tokenHash);

    // Permite eliminar sesiones por token (Logout)
    void deleteByTokenHash(String tokenHash);

    // Permite cerrar todas las sesiones de un usuario (Logout global)
    void deleteByUser(User user);
}
