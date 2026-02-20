package com.openroof.openroof.model.property;

import com.openroof.openroof.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Superclase abstracta para features (interior/exterior).
 * Comparten: name (unique) + categoría (definida en subclase).
 */
@Getter
@Setter
@MappedSuperclass
public abstract class AbstractFeature extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}
