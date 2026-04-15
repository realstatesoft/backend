package com.openroof.openroof.model.preference;

import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Preferencias de búsqueda guardadas por un usuario.
 * Relación 1:1 con User (una preferencia por usuario).
 */
@Entity
@Table(name = "user_preferences")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private boolean onboardingCompleted = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_preference_options",
        joinColumns = @JoinColumn(name = "user_preference_id"),
        inverseJoinColumns = @JoinColumn(name = "preference_option_id")
    )
    @Builder.Default
    private Set<PreferenceOption> selectedOptions = new HashSet<>();

    @OneToMany(mappedBy = "userPreference", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserPreferenceRange> ranges = new ArrayList<>();
}
