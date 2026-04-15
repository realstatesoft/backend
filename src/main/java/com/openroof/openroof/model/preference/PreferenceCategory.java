package com.openroof.openroof.model.preference;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Categoría del catálogo de preferencias (ej: PROPERTY_TYPE, ZONE, EXTERIOR_FEATURE).
 * Entidad de catálogo estático — sin soft-delete ni auditoría.
 */
@Entity
@Table(name = "preference_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    private List<PreferenceOption> options = new ArrayList<>();
}
