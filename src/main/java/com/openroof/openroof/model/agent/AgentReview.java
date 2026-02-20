package com.openroof.openroof.model.agent;

import com.openroof.openroof.common.AbstractReview;
import com.openroof.openroof.model.property.Property;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agent_reviews", indexes = {
        @Index(name = "idx_agent_reviews_agent", columnList = "agent_id"),
        @Index(name = "idx_agent_reviews_rating", columnList = "rating"),
        @Index(name = "idx_agent_reviews_date", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentReview extends AbstractReview {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentProfile agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "property_id")
    private Property property;
}
