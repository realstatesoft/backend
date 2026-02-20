package com.openroof.openroof.common.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Value object genérico para rangos de enteros (min/max).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IntegerRange {

    @Column
    private Integer min;

    @Column
    private Integer max;

    public boolean contains(int value) {
        return (min == null || value >= min) && (max == null || value <= max);
    }
}
