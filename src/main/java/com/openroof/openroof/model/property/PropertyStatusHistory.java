package com.openroof.openroof.model.property;

import com.openroof.openroof.model.enums.PropertyStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "property_status_history", indexes = {
        @Index(name = "idx_property_status_history_property", columnList = "property_id"),
        @Index(name = "idx_property_status_history_date", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyStatusHistory extends AbstractPropertyHistoryEntry {

    @Enumerated(EnumType.STRING)
    @Column(name = "old_status", length = 50)
    private PropertyStatus oldStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 50)
    private PropertyStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;
}
