package com.openroof.openroof.model.property;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.common.embeddable.ConstructionDetails;
import com.openroof.openroof.common.embeddable.GeoLocation;
import com.openroof.openroof.common.embeddable.UtilityInfo;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.*;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "properties", indexes = {
        @Index(name = "idx_properties_owner", columnList = "owner_id"),
        @Index(name = "idx_properties_agent", columnList = "agent_id"),
        @Index(name = "idx_properties_location", columnList = "location_id"),
        @Index(name = "idx_properties_type", columnList = "property_type"),
        @Index(name = "idx_properties_listing", columnList = "status, visibility, deleted_at"),
        @Index(name = "idx_properties_price", columnList = "price"),
        @Index(name = "idx_properties_highlighted", columnList = "highlighted"),
        @Index(name = "idx_properties_created", columnList = "created_at")
})
@SQLRestriction("deleted_at IS NULL AND trashed_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property extends BaseEntity {

    // ─── Relaciones ───────────────────────────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private AgentProfile agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    // ─── Información básica ───────────────────────────────────────

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private PropertyCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "property_type", nullable = false, length = 50)
    private PropertyType propertyType;

    // ─── Ubicación ────────────────────────────────────────────────

    @Column(nullable = false, length = 500)
    private String address;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "lat", precision = 10, scale = 8)),
            @AttributeOverride(name = "lng", column = @Column(name = "lng", precision = 11, scale = 8))
    })
    private GeoLocation geoLocation;

    // ─── Precio y características ─────────────────────────────────

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Builder.Default
    private Integer bedrooms = 0;

    @Column(precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal bathrooms = BigDecimal.ZERO;

    @Column(name = "half_bathrooms")
    @Builder.Default
    private Integer halfBathrooms = 0;

    @Column(name = "full_bathrooms")
    @Builder.Default
    private Integer fullBathrooms = 0;

    @Column(name = "surface_area", precision = 10, scale = 2)
    private BigDecimal surfaceArea;

    @Column(name = "built_area", precision = 10, scale = 2)
    private BigDecimal builtArea;

    @Column(name = "parking_spaces")
    @Builder.Default
    private Integer parkingSpaces = 0;

    @Column(name = "floors_count")
    @Builder.Default
    private Integer floorsCount = 1;

    // ─── Detalles de construcción (Value Object) ──────────────────

    @Embedded
    private ConstructionDetails construction;

    // ─── Servicios (Value Object) ─────────────────────────────────

    @Embedded
    private UtilityInfo utilities;

    // ─── Estado y visibilidad ─────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private PropertyStatus status = PropertyStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Visibility visibility = Visibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private Availability availability = Availability.IMMEDIATE;

    // ─── Destacados y métricas ────────────────────────────────────

    @Builder.Default
    private Boolean highlighted = false;

    @Column(name = "highlighted_until")
    private LocalDateTime highlightedUntil;

    @Column(name = "view_count")
    @Builder.Default
    private Integer viewCount = 0;

    @Column(name = "favorite_count")
    @Builder.Default
    private Integer favoriteCount = 0;

    // ─── Publicación ─────────────────────────────────────────────

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // ─── Papelera de Reciclaje ────────────────────────────────────
    @Column(name = "trashed_at")
    private LocalDateTime trashedAt;

    // ─── Colecciones ──────────────────────────────────────────────

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PropertyRoom> rooms = new ArrayList<>();

    @OneToMany(mappedBy = "property", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PropertyMedia> media = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "property_exterior_features",
            joinColumns = @JoinColumn(name = "property_id"),
            inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    @Builder.Default
    private List<ExteriorFeature> exteriorFeatures = new ArrayList<>();
}
