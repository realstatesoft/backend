package com.openroof.openroof.mapper;

import com.openroof.openroof.common.embeddable.IntegerRange;
import com.openroof.openroof.common.embeddable.MoneyRange;
import com.openroof.openroof.dto.agent.AgentClientResponse;
import com.openroof.openroof.dto.agent.AgentClientSummaryResponse;
import com.openroof.openroof.dto.agent.CreateAgentClientRequest;
import com.openroof.openroof.dto.agent.UpdateAgentClientRequest;
import com.openroof.openroof.model.agent.AgentClient;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ClientType;
import com.openroof.openroof.model.enums.ContactMethod;
import com.openroof.openroof.model.enums.Priority;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@code AgentClientMapper}.
 */
@DisplayName("AgentClientMapper")
class AgentClientMapperTest {

    private final AgentClientMapper mapper = new AgentClientMapper();

    // ─── toResponse ──────────────────────────────────────────────────────────

    /**
     * toResponse_withFullData_mapsAllFields.
     */
    @Test
    @DisplayName("toResponse_withFullData_mapsAllFields")
    void toResponse_withFullData_mapsAllFields() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(10L);
        when(user.getName()).thenReturn("Juan Perez");
        when(user.getEmail()).thenReturn("juan@example.com");
        when(user.getPhone()).thenReturn("+5491100000000");

        User agentUser = mock(User.class);
        when(agentUser.getName()).thenReturn("Agent Name");
        AgentProfile agent = mock(AgentProfile.class);
        when(agent.getId()).thenReturn(5L);
        when(agent.getUser()).thenReturn(agentUser);

        LocalDateTime lastContact = LocalDateTime.of(2024, 1, 15, 10, 0);

        AgentClient ac = AgentClient.builder()
                .agent(agent)
                .user(user)
                .status(ClientStatus.ACTIVE)
                .priority(Priority.HIGH)
                .tags(List.of("investor", "vip"))
                .visitedPropertiesCount(3)
                .offersCount(1)
                .budgetRange(new MoneyRange(BigDecimal.valueOf(100_000), BigDecimal.valueOf(500_000)))
                .bedroomRange(new IntegerRange(2, 4))
                .bathroomRange(new IntegerRange(1, 2))
                .preferredContactMethod(ContactMethod.WHATSAPP)
                .lastContactDate(lastContact)
                .notes("Interested in downtown")
                .occupation("Engineer")
                .annualIncome(BigDecimal.valueOf(120_000))
                .isSearchingProperty(true)
                .build();

        AgentClientResponse resp = mapper.toResponse(ac);

        assertThat(resp.agentId()).isEqualTo(5L);
        assertThat(resp.agentName()).isEqualTo("Agent Name");
        assertThat(resp.userId()).isEqualTo(10L);
        assertThat(resp.userName()).isEqualTo("Juan Perez");
        assertThat(resp.userEmail()).isEqualTo("juan@example.com");
        assertThat(resp.userPhone()).isEqualTo("+5491100000000");
        assertThat(resp.status()).isEqualTo("ACTIVE");
        assertThat(resp.priority()).isEqualTo("HIGH");
        assertThat(resp.tags()).containsExactly("investor", "vip");
        assertThat(resp.visitedPropertiesCount()).isEqualTo(3);
        assertThat(resp.offersCount()).isEqualTo(1);
        assertThat(resp.minBudget()).isEqualByComparingTo("100000");
        assertThat(resp.maxBudget()).isEqualByComparingTo("500000");
        assertThat(resp.minBedrooms()).isEqualTo(2);
        assertThat(resp.maxBedrooms()).isEqualTo(4);
        assertThat(resp.minBathrooms()).isEqualTo(1);
        assertThat(resp.maxBathrooms()).isEqualTo(2);
        assertThat(resp.preferredContactMethod()).isEqualTo("WHATSAPP");
        assertThat(resp.lastContactAt()).isEqualTo(lastContact);
        assertThat(resp.notes()).isEqualTo("Interested in downtown");
        assertThat(resp.occupation()).isEqualTo("Engineer");
        assertThat(resp.annualIncome()).isEqualByComparingTo("120000");
        assertThat(resp.isSearchingProperty()).isTrue();
    }

    /**
     * toResponse_withNullUserAndAgent_returnsNullUserAgentFields.
     */
    @Test
    @DisplayName("toResponse_withNullUserAndAgent_returnsNullUserAgentFields")
    void toResponse_withNullUserAndAgent_returnsNullUserAgentFields() {
        AgentClient ac = AgentClient.builder().build();

        AgentClientResponse resp = mapper.toResponse(ac);

        assertThat(resp.userId()).isNull();
        assertThat(resp.userName()).isNull();
        assertThat(resp.userEmail()).isNull();
        assertThat(resp.userPhone()).isNull();
        assertThat(resp.agentId()).isNull();
        assertThat(resp.agentName()).isNull();
    }

    /**
     * toResponse_withNullRanges_returnsNullRangeFields.
     */
    @Test
    @DisplayName("toResponse_withNullRanges_returnsNullRangeFields")
    void toResponse_withNullRanges_returnsNullRangeFields() {
        AgentClient ac = AgentClient.builder()
                .budgetRange(null)
                .bedroomRange(null)
                .bathroomRange(null)
                .build();

        AgentClientResponse resp = mapper.toResponse(ac);

        assertThat(resp.minBudget()).isNull();
        assertThat(resp.maxBudget()).isNull();
        assertThat(resp.minBedrooms()).isNull();
        assertThat(resp.maxBedrooms()).isNull();
        assertThat(resp.minBathrooms()).isNull();
        assertThat(resp.maxBathrooms()).isNull();
    }

    /**
     * toResponse_withAgentButNullAgentUser_returnsNullAgentName.
     */
    @Test
    @DisplayName("toResponse_withAgentButNullAgentUser_returnsNullAgentName")
    void toResponse_withAgentButNullAgentUser_returnsNullAgentName() {
        AgentProfile agent = mock(AgentProfile.class);
        when(agent.getId()).thenReturn(7L);
        when(agent.getUser()).thenReturn(null);

        AgentClient ac = AgentClient.builder().agent(agent).build();

        AgentClientResponse resp = mapper.toResponse(ac);

        assertThat(resp.agentId()).isEqualTo(7L);
        assertThat(resp.agentName()).isNull();
    }

    /**
     * toResponse_withNullEnums_returnsNullEnumStrings.
     */
    @Test
    @DisplayName("toResponse_withNullEnums_returnsNullEnumStrings")
    void toResponse_withNullEnums_returnsNullEnumStrings() {
        AgentClient ac = AgentClient.builder()
                .status(null)
                .priority(null)
                .preferredContactMethod(null)
                .build();

        AgentClientResponse resp = mapper.toResponse(ac);

        assertThat(resp.status()).isNull();
        assertThat(resp.priority()).isNull();
        assertThat(resp.preferredContactMethod()).isNull();
    }

    // ─── toSummaryResponse ───────────────────────────────────────────────────

    /**
     * toSummaryResponse_withFullUser_mapsAllFields.
     */
    @Test
    @DisplayName("toSummaryResponse_withFullUser_mapsAllFields")
    void toSummaryResponse_withFullUser_mapsAllFields() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(20L);
        when(user.getName()).thenReturn("Maria Lopez");
        when(user.getEmail()).thenReturn("maria@example.com");
        when(user.getPhone()).thenReturn("+549111");

        LocalDateTime lastContact = LocalDateTime.of(2024, 3, 10, 9, 0);

        AgentClient ac = AgentClient.builder()
                .user(user)
                .status(ClientStatus.CONVERTED)
                .priority(Priority.LOW)
                .clientType(ClientType.BUYER)
                .lastContactDate(lastContact)
                .build();

        AgentClientSummaryResponse resp = mapper.toSummaryResponse(ac);

        assertThat(resp.userId()).isEqualTo(20L);
        assertThat(resp.userName()).isEqualTo("Maria Lopez");
        assertThat(resp.userEmail()).isEqualTo("maria@example.com");
        assertThat(resp.userPhone()).isEqualTo("+549111");
        assertThat(resp.status()).isEqualTo("CONVERTED");
        assertThat(resp.priority()).isEqualTo("LOW");
        assertThat(resp.clientType()).isEqualTo("BUYER");
        assertThat(resp.lastContactAt()).isEqualTo(lastContact);
    }

    /**
     * toSummaryResponse_withNullUser_returnsNullUserFields.
     */
    @Test
    @DisplayName("toSummaryResponse_withNullUser_returnsNullUserFields")
    void toSummaryResponse_withNullUser_returnsNullUserFields() {
        AgentClient ac = AgentClient.builder().build();

        AgentClientSummaryResponse resp = mapper.toSummaryResponse(ac);

        assertThat(resp.userId()).isNull();
        assertThat(resp.userName()).isNull();
        assertThat(resp.userEmail()).isNull();
        assertThat(resp.userPhone()).isNull();
    }

    // ─── toEntity ────────────────────────────────────────────────────────────

    /**
     * toEntity_withBudgetBedroomBathroomRanges_buildsRanges.
     */
    @Test
    @DisplayName("toEntity_withBudgetBedroomBathroomRanges_buildsRanges")
    void toEntity_withBudgetBedroomBathroomRanges_buildsRanges() {
        CreateAgentClientRequest req = new CreateAgentClientRequest(
                1L, 2L, null,
                ClientStatus.ACTIVE, Priority.MEDIUM, null,
                BigDecimal.valueOf(50_000), BigDecimal.valueOf(200_000),
                2, 4,
                1, 2,
                ContactMethod.EMAIL,
                null, null, null, null, null, null, null, null, null, null);

        AgentProfile agent = mock(AgentProfile.class);
        User user = mock(User.class);

        AgentClient entity = mapper.toEntity(req, agent, user);

        assertThat(entity.getAgent()).isEqualTo(agent);
        assertThat(entity.getUser()).isEqualTo(user);
        assertThat(entity.getBudgetRange().getMin()).isEqualByComparingTo("50000");
        assertThat(entity.getBudgetRange().getMax()).isEqualByComparingTo("200000");
        assertThat(entity.getBedroomRange().getMin()).isEqualTo(2);
        assertThat(entity.getBedroomRange().getMax()).isEqualTo(4);
        assertThat(entity.getBathroomRange().getMin()).isEqualTo(1);
        assertThat(entity.getBathroomRange().getMax()).isEqualTo(2);
    }

    /**
     * toEntity_withNullRanges_doesNotSetRanges.
     */
    @Test
    @DisplayName("toEntity_withNullRanges_doesNotSetRanges")
    void toEntity_withNullRanges_doesNotSetRanges() {
        CreateAgentClientRequest req = new CreateAgentClientRequest(
                1L, 2L, null,
                null, null, null,
                null, null,
                null, null,
                null, null,
                null,
                null, null, null, null, null, null, null, null, null, null);

        AgentClient entity = mapper.toEntity(req, null, null);

        assertThat(entity.getBudgetRange()).isNull();
        assertThat(entity.getBedroomRange()).isNull();
        assertThat(entity.getBathroomRange()).isNull();
    }

    /**
     * toEntity_withOptionalPersonalFields_mapsThemToEntity.
     */
    @Test
    @DisplayName("toEntity_withOptionalPersonalFields_mapsThemToEntity")
    void toEntity_withOptionalPersonalFields_mapsThemToEntity() {
        CreateAgentClientRequest req = new CreateAgentClientRequest(
                1L, 2L, null,
                ClientStatus.INACTIVE, Priority.HIGH,
                List.of("tagA"),
                null, null, null, null, null, null,
                ContactMethod.PHONE,
                LocalDate.of(1990, 5, 15), null, "Developer",
                BigDecimal.valueOf(80_000), "Calle Falsa 123", "REFERRAL",
                List.of("HOUSE"), List.of("Palermo"), List.of("Pool"), "some notes");

        AgentClient entity = mapper.toEntity(req, null, null);

        assertThat(entity.getStatus()).isEqualTo(ClientStatus.INACTIVE);
        assertThat(entity.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(entity.getTags()).containsExactly("tagA");
        assertThat(entity.getPreferredContactMethod()).isEqualTo(ContactMethod.PHONE);
        assertThat(entity.getBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(entity.getOccupation()).isEqualTo("Developer");
        assertThat(entity.getAnnualIncome()).isEqualByComparingTo("80000");
        assertThat(entity.getAddress()).isEqualTo("Calle Falsa 123");
        assertThat(entity.getSourceChannel()).isEqualTo("REFERRAL");
        assertThat(entity.getPreferredPropertyTypes()).containsExactly("HOUSE");
        assertThat(entity.getPreferredAreas()).containsExactly("Palermo");
        assertThat(entity.getDesiredFeatures()).containsExactly("Pool");
        assertThat(entity.getNotes()).isEqualTo("some notes");
    }

    // ─── updateEntity ────────────────────────────────────────────────────────

    /**
     * updateEntity_withAllFields_updatesAllMutableFields.
     */
    @Test
    @DisplayName("updateEntity_withAllFields_updatesAllMutableFields")
    void updateEntity_withAllFields_updatesAllMutableFields() {
        AgentClient ac = AgentClient.builder()
                .status(ClientStatus.ACTIVE)
                .priority(Priority.LOW)
                .build();

        UpdateAgentClientRequest req = new UpdateAgentClientRequest(
                null, null, null, null,
                ClientStatus.CONVERTED, Priority.HIGH,
                List.of("vip"),
                null, null, null, null, null, null,
                ContactMethod.WHATSAPP,
                null, null, "Architect",
                BigDecimal.valueOf(90_000), "Av. Corrientes 1234", "WEB",
                List.of("APARTMENT"), List.of("Belgrano"), List.of("Gym"),
                "updated notes", Boolean.TRUE);

        mapper.updateEntity(ac, req);

        assertThat(ac.getStatus()).isEqualTo(ClientStatus.CONVERTED);
        assertThat(ac.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(ac.getTags()).containsExactly("vip");
        assertThat(ac.getPreferredContactMethod()).isEqualTo(ContactMethod.WHATSAPP);
        assertThat(ac.getOccupation()).isEqualTo("Architect");
        assertThat(ac.getAnnualIncome()).isEqualByComparingTo("90000");
        assertThat(ac.getAddress()).isEqualTo("Av. Corrientes 1234");
        assertThat(ac.getSourceChannel()).isEqualTo("WEB");
        assertThat(ac.getPreferredPropertyTypes()).containsExactly("APARTMENT");
        assertThat(ac.getPreferredAreas()).containsExactly("Belgrano");
        assertThat(ac.getDesiredFeatures()).containsExactly("Gym");
        assertThat(ac.getNotes()).isEqualTo("updated notes");
        assertThat(ac.getIsSearchingProperty()).isTrue();
    }

    /**
     * updateEntity_mergeBudgetRange_withExistingRange_updatesMinOnly.
     */
    @Test
    @DisplayName("updateEntity_mergeBudgetRange_withExistingRange_updatesMinOnly")
    void updateEntity_mergeBudgetRange_withExistingRange_updatesMinOnly() {
        AgentClient ac = AgentClient.builder()
                .budgetRange(new MoneyRange(BigDecimal.valueOf(100_000), BigDecimal.valueOf(300_000)))
                .build();

        UpdateAgentClientRequest req = new UpdateAgentClientRequest(
                null, null, null, null, null, null, null,
                BigDecimal.valueOf(150_000), null,   // only minBudget
                null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);

        mapper.updateEntity(ac, req);

        assertThat(ac.getBudgetRange().getMin()).isEqualByComparingTo("150000");
        assertThat(ac.getBudgetRange().getMax()).isEqualByComparingTo("300000"); // unchanged
    }

    /**
     * updateEntity_mergeBudgetRange_withBothNull_keepsExistingRange.
     */
    @Test
    @DisplayName("updateEntity_mergeBudgetRange_withBothNull_keepsExistingRange")
    void updateEntity_mergeBudgetRange_withBothNull_keepsExistingRange() {
        MoneyRange original = new MoneyRange(BigDecimal.valueOf(50_000), BigDecimal.valueOf(100_000));
        AgentClient ac = AgentClient.builder().budgetRange(original).build();

        UpdateAgentClientRequest req = new UpdateAgentClientRequest(
                null, null, null, null, null, null, null,
                null, null,
                null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);

        mapper.updateEntity(ac, req);

        assertThat(ac.getBudgetRange()).isSameAs(original);
    }

    /**
     * updateEntity_mergeBedroomRange_withNullExisting_createsNewRange.
     */
    @Test
    @DisplayName("updateEntity_mergeBedroomRange_withNullExisting_createsNewRange")
    void updateEntity_mergeBedroomRange_withNullExisting_createsNewRange() {
        AgentClient ac = AgentClient.builder().bedroomRange(null).build();

        UpdateAgentClientRequest req = new UpdateAgentClientRequest(
                null, null, null, null, null, null, null,
                null, null,
                3, null,  // only minBedrooms
                null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);

        mapper.updateEntity(ac, req);

        assertThat(ac.getBedroomRange()).isNotNull();
        assertThat(ac.getBedroomRange().getMin()).isEqualTo(3);
        assertThat(ac.getBedroomRange().getMax()).isNull();
    }
}
