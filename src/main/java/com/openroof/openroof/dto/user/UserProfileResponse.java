package com.openroof.openroof.dto.user;

import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta con los datos del perfil del usuario autenticado.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserProfileResponse {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private String avatarUrl;
    private UserRole role;

    public static UserProfileResponse from(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .build();
    }
}
