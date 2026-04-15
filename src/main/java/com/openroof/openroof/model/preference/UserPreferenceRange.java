package com.openroof.openroof.model.preference;

import jakarta.persistence.*;
import lombok.*;

/**
 * Rango numérico dentro de una preferencia de usuario.
 * Ejemplos: PRICE [100000, 300000], SURFACE [80, 200], BEDROOMS [2, 4].
 */
@Entity
@Table(name = "user_preference_ranges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferenceRange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_preference_id", nullable = false)
    private UserPreference userPreference;

    @Column(name = "field_name", nullable = false, length = 50)
    private String fieldName;

    @Column(name = "min_value")
    private Double minValue;

    @Column(name = "max_value")
    private Double maxValue;
}
