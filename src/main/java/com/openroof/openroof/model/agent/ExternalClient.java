package com.openroof.openroof.model.agent;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.common.embeddable.IntegerRange;
import com.openroof.openroof.common.embeddable.MoneyRange;
import com.openroof.openroof.model.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "external_clients", indexes = {
        @Index(name = "idx_external_clients_agent", columnList = "agent_id"),
        @Index(name = "idx_external_clients_status", columnList = "status"),
        @Index(name = "idx_external_clients_priority", columnList = "priority")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExternalClient extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private AgentProfile agent;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

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
    @Builder.Default
    private ClientType clientType = ClientType.INDIVIDUAL;

    @Column(name = "last_contact_date")
    private LocalDateTime lastContactDate;

    @Column(length = 100)
    private String origin;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String notes;

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
    @Column(name = "marital_status", length = 20)
    private MaritalStatus maritalStatus;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 255)
    private String occupation;

    @Column(name = "annual_income", precision = 19, scale = 2)
    private java.math.BigDecimal annualIncome;

    @Column(columnDefinition = "TEXT")
    private String address;

    @Column(name = "source_channel", length = 100)
    private String sourceChannel;

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

    @Column(name = "is_searching_property")
    @Builder.Default
    private Boolean isSearchingProperty = false;
}
