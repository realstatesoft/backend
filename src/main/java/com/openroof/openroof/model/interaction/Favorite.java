package com.openroof.openroof.model.interaction;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "favorites", indexes = {
        @Index(name = "idx_favorites_user", columnList = "user_id"),
        @Index(name = "idx_favorites_property", columnList = "property_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "idx_favorites_unique", columnNames = {"user_id", "property_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Favorite extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;
}
