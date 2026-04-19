package com.openroof.openroof.service;

import com.openroof.openroof.dto.user.UpdateUserRequest;
import com.openroof.openroof.dto.user.UserProfileResponse;
import com.openroof.openroof.dto.user.UserSearchResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    public static final LocalDateTime INDEFINITE_SUSPENSION_DATE = LocalDateTime.of(9999, 12, 31, 23, 59, 59);

    private final UserRepository userRepository;

    /**
     * Retorna el perfil del usuario autenticado.
     */
    public UserProfileResponse getProfile(String email) {
        User user = findByEmail(email);
        return UserProfileResponse.from(user);
    }

    /**
     * Actualiza los datos personales del usuario autenticado.
     * Solo actualiza los campos que no sean null en el request.
     */
    @Transactional
    public UserProfileResponse updatePersonalData(String email, UpdateUserRequest request) {
        User user = findByEmail(email);

        if (request.getName() != null) {
            user.setName(request.getName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userRepository.save(user);
        return UserProfileResponse.from(user);
    }

    /**
     * Busca un usuario por email, ignorando mayúsculas/minúsculas. Usado por agentes para vincular clientes existentes.
     */
    public Optional<UserSearchResponse> searchByEmail(String email) {
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .map(u -> new UserSearchResponse(u.getId(), u.getName(), u.getEmail()));
    }

    // ─── helper ───────────────────────────────────────────────────────────────
    // ─── Suspensión ───────────────────────────────────────────────────────────

    /**
     * Suspende un usuario hasta la fecha indicada.
     * Si suspendedUntil es null, la suspensión es indefinida.
     * No se puede suspender a un administrador.
     */
    @Transactional
    public void suspendUser(Long userId, LocalDateTime suspendedUntil, String reason) {
        User user = findById(userId);

        if (user.getRole() == UserRole.ADMIN) {
            throw new IllegalArgumentException("No se puede suspender a un administrador");
        }

        if (suspendedUntil != null && suspendedUntil.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("La fecha de suspensión debe ser en el futuro");
        }

        user.setSuspendedUntil(suspendedUntil != null ? suspendedUntil : INDEFINITE_SUSPENSION_DATE);
        user.setSuspensionReason(reason);
        userRepository.save(user);
    }

    /**
     * Levanta la suspensión de un usuario.
     */
    @Transactional
    public void unsuspendUser(Long userId) {
        User user = findById(userId);
        user.setSuspendedUntil(null);
        user.setSuspensionReason(null);
        userRepository.save(user);
    }

    /**
     * Devuelve true si el usuario está actualmente suspendido
     * (suspendedUntil != null y es posterior al momento actual).
     */
    public boolean isUserSuspended(User user) {
        return user.getSuspendedUntil() != null
                && user.getSuspendedUntil().isAfter(LocalDateTime.now());
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + userId));
    }
}
