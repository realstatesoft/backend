package com.openroof.openroof.model.preference;

import jakarta.persistence.*;
import lombok.*;

/**
 * Opción individual dentro de una categoría de preferencias
 * (ej: label="Casa", value="HOUSE" dentro de PROPERTY_TYPE).
 */
@Entity
@Table(name = "preference_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PreferenceOption {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private PreferenceCategory category;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, length = 100)
    private String value;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
}
