package com.openroof.openroof.model.agent;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.common.embeddable.IntegerRange;
import com.openroof.openroof.common.embeddable.MoneyRange;
import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ClientType;
import com.openroof.openroof.model.enums.ContactMethod;
import com.openroof.openroof.model.enums.MaritalStatus;
import com.openroof.openroof.model.enums.Priority;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "agent_clients", indexes = {
        @Index(name = "idx_agent_clients_agent", columnList = "agent_id"),
        @Index(name = "idx_agent_clients_user", columnList = "user_id"),
        @Index(name = "idx_agent_clients_status", columnList = "status"),
        @Index(name = "idx_agent_clients_priority", columnList = "priority"),
        @Index(name = "idx_agent_clients_client_type", columnList = "client_type")
})
@SQLRestriction("deleted_at IS NULL")
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
    
    @Enumerated(EnumType.STRING)
    @Column(name = "client_type", length = 20)
    private ClientType clientType;

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

    @Column(name = "last_contact_at")

    private LocalDateTime lastContactDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status", length = 20)
    private MaritalStatus maritalStatus;

    @Column(name = "birth_date")
    private java.time.LocalDate birthDate;

    @Column(length = 255)
    private String occupation;

    @Column(name = "annual_income", precision = 19, scale = 2)
    private java.math.BigDecimal annualIncome;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "source_channel", length = 100)
    private String sourceChannel;

    @Column(name = "interactions_count")
    @Builder.Default
    private Integer interactionsCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_property_types", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> preferredPropertyTypes = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_areas", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> preferredAreas = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "desired_features", columnDefinition = "jsonb")
    @Builder.Default
    private List<String> desiredFeatures = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_searching_property")
    @Builder.Default
    private Boolean isSearchingProperty = false;
}
