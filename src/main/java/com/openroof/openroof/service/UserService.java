package com.openroof.openroof.service;

import com.openroof.openroof.dto.user.UpdateUserRequest;
import com.openroof.openroof.dto.user.UserProfileResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

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

        user.setSuspendedUntil(suspendedUntil != null ? suspendedUntil : LocalDateTime.MAX);
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
