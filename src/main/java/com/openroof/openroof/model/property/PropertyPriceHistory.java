package com.openroof.openroof.model.property;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "property_price_history", indexes = {
        @Index(name = "idx_property_price_history_property", columnList = "property_id"),
        @Index(name = "idx_property_price_history_date", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyPriceHistory extends AbstractPropertyHistoryEntry {

    @Column(name = "old_price", precision = 12, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal newPrice;
}
