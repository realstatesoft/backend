package com.openroof.openroof.common.embeddable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

/**
 * Value object para rangos monetarios (min/max).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoneyRange {

    @Column(precision = 12, scale = 2)
    private BigDecimal min;

    @Column(precision = 12, scale = 2)
    private BigDecimal max;

    public boolean contains(BigDecimal value) {
        if (value == null) return false;
        return (min == null || value.compareTo(min) >= 0)
                && (max == null || value.compareTo(max) <= 0);
    }
}
