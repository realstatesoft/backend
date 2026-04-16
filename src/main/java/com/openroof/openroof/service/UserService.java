package com.openroof.openroof.service;

import com.openroof.openroof.dto.user.UpdateUserRequest;
import com.openroof.openroof.dto.user.UserProfileResponse;
import com.openroof.openroof.dto.user.UserSearchResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    /**
     * Busca un usuario por email, ignorando mayúsculas/minúsculas. Usado por agentes para vincular clientes existentes.
     */
    public Optional<UserSearchResponse> searchByEmail(String email) {
        return userRepository.findByEmailIgnoreCaseAndDeletedAtIsNull(email)
                .map(u -> new UserSearchResponse(u.getId(), u.getName(), u.getEmail()));
    }

    // ─── helper ───────────────────────────────────────────────────────────────

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }
}
