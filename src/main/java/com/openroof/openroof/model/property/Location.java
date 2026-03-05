package com.openroof.openroof.model.property;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.common.embeddable.GeoLocation;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "locations", indexes = {
        @Index(name = "idx_locations_name", columnList = "name"),
        @Index(name = "idx_locations_city_dept", columnList = "city, department")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location extends BaseEntity {

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(length = 100)
    @Builder.Default
    private String country = "Paraguay";

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "lat", column = @Column(name = "lat", precision = 10, scale = 8)),
            @AttributeOverride(name = "lng", column = @Column(name = "lng", precision = 11, scale = 8))
    })
    private GeoLocation geoLocation;
}
