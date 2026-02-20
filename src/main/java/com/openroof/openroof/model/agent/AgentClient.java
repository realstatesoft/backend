package com.openroof.openroof.model.agent;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.common.embeddable.IntegerRange;
import com.openroof.openroof.common.embeddable.MoneyRange;
import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ContactMethod;
import com.openroof.openroof.model.enums.Priority;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agent_clients", indexes = {
        @Index(name = "idx_agent_clients_agent", columnList = "agent_id"),
        @Index(name = "idx_agent_clients_user", columnList = "user_id"),
        @Index(name = "idx_agent_clients_status", columnList = "status"),
        @Index(name = "idx_agent_clients_priority", columnList = "priority")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentClient extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentProfile agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private ClientStatus status = ClientStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private Priority priority = Priority.MEDIUM;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(name = "visited_properties_count")
    @Builder.Default
    private Integer visitedPropertiesCount = 0;

    @Column(name = "offers_count")
    @Builder.Default
    private Integer offersCount = 0;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_budget")),
            @AttributeOverride(name = "max", column = @Column(name = "max_budget"))
    })
    private MoneyRange budgetRange;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_bedrooms")),
            @AttributeOverride(name = "max", column = @Column(name = "max_bedrooms"))
    })
    private IntegerRange bedroomRange;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "min", column = @Column(name = "min_bathrooms")),
            @AttributeOverride(name = "max", column = @Column(name = "max_bathrooms"))
    })
    private IntegerRange bathroomRange;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_contact_method", length = 20)
    @Builder.Default
    private ContactMethod preferredContactMethod = ContactMethod.EMAIL;

    @Column(name = "last_contact_date")
    private LocalDateTime lastContactDate;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
