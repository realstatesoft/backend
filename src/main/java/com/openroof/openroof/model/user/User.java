package com.openroof.openroof.model.user;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.ClientType;
import com.openroof.openroof.model.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email", columnList = "email"),
        @Index(name = "idx_users_role", columnList = "role"),
        @Index(name = "idx_users_suspended", columnList = "suspended_until")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 100)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 20)
    private ClientType clientType;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;

    @Column(name = "suspension_reason", columnDefinition = "TEXT")
    private String suspensionReason;

    /*
     * Auth: Enrique Rios
     * Desc:
     * define la lógica de autenticación (email/password), los permisos basados en
     * roles
     * ultima modif: 21/02/2026
     */

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    // ======= Métodos de estado de cuenta =======//
    // necesario porque se implementa la interfaz UserDetails
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        // Si no hay fecha de suspensión o la fecha ya pasó, la cuenta NO está
        // bloqueada.
        return suspendedUntil == null || suspendedUntil.isBefore(LocalDateTime.now());
    }

    // Indica si la contraseña ha expirado y debe cambiarse.
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // Verifica si la cuenta está habilitada (ej. si el usuario confirmó su email).
    @Override
    public boolean isEnabled() {
        return true;
    }
}
