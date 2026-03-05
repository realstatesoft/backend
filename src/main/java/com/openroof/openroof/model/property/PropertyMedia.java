package com.openroof.openroof.model.property;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "property_media", indexes = {
        @Index(name = "idx_property_media_property", columnList = "property_id"),
        @Index(name = "idx_property_media_primary", columnList = "property_id, is_primary"),
        @Index(name = "idx_property_media_order", columnList = "property_id, order_index")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyMedia extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MediaType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "order_index")
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(length = 255)
    private String title;
}
