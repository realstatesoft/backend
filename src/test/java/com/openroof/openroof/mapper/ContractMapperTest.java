package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.contract.ContractResponse;
import com.openroof.openroof.dto.contract.ContractSummaryResponse;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ContractMapper")
class ContractMapperTest {

    private final ContractMapper mapper = new ContractMapper();

    // ─── toResponse — commission calculations ────────────────────────────────

    @Test
    @DisplayName("toResponse_withAmount_calculatesCommissionAmountsCorrectly")
    void toResponse_withAmount_calculatesCommissionAmountsCorrectly() {
        // amount=1_000_000, totalPct=6%, listingPct=3.5%, buyerPct=2.5%
        Contract contract = Contract.builder()
                .amount(new BigDecimal("1000000"))
                .commissionPct(new BigDecimal("6.00"))
                .listingAgentCommissionPct(new BigDecimal("3.50"))
                .buyerAgentCommissionPct(new BigDecimal("2.50"))
                .contractType(ContractType.SALE)
                .status(ContractStatus.DRAFT)
                .build();

        ContractResponse dto = mapper.toResponse(contract);

        // totalAmt = 1_000_000 * 6 / 100 = 60_000
        assertThat(dto.totalCommissionAmount()).isEqualByComparingTo("60000.00");
        // listingAmt = 1_000_000 * 3.5 / 100 = 35_000
        assertThat(dto.listingAgentCommissionAmount()).isEqualByComparingTo("35000.00");
        // buyerAmt = 1_000_000 * 2.5 / 100 = 25_000
        assertThat(dto.buyerAgentCommissionAmount()).isEqualByComparingTo("25000.00");
        // percentages passed through
        assertThat(dto.commissionPct()).isEqualByComparingTo("6.00");
        assertThat(dto.listingAgentCommissionPct()).isEqualByComparingTo("3.50");
        assertThat(dto.buyerAgentCommissionPct()).isEqualByComparingTo("2.50");
    }

    @Test
    @DisplayName("toResponse_nullAmount_treatedAsZeroForCommissions")
    void toResponse_nullAmount_treatedAsZeroForCommissions() {
        Contract contract = Contract.builder()
                .amount(null)
                .commissionPct(new BigDecimal("5.00"))
                .listingAgentCommissionPct(new BigDecimal("5.00"))
                .buyerAgentCommissionPct(BigDecimal.ZERO)
                .contractType(ContractType.RENT)
                .status(ContractStatus.DRAFT)
                .build();

        ContractResponse dto = mapper.toResponse(contract);

        assertThat(dto.amount()).isEqualByComparingTo("0");
        assertThat(dto.totalCommissionAmount()).isEqualByComparingTo("0.00");
        assertThat(dto.listingAgentCommissionAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("toResponse_nullCommissionPct_treatedAsZero")
    void toResponse_nullCommissionPct_treatedAsZero() {
        Contract contract = Contract.builder()
                .amount(new BigDecimal("500000"))
                .commissionPct(null)
                .listingAgentCommissionPct(null)
                .buyerAgentCommissionPct(null)
                .contractType(ContractType.SALE)
                .status(ContractStatus.DRAFT)
                .build();

        ContractResponse dto = mapper.toResponse(contract);

        assertThat(dto.commissionPct()).isEqualByComparingTo("0");
        assertThat(dto.totalCommissionAmount()).isEqualByComparingTo("0.00");
    }

    // ─── toResponse — null FK guards ────────────────────────────────────────

    @Test
    @DisplayName("toResponse_nullProperty_returnsNullPropertyFields")
    void toResponse_nullProperty_returnsNullPropertyFields() {
        Contract contract = Contract.builder()
                .amount(BigDecimal.TEN)
                .commissionPct(BigDecimal.ZERO)
                .listingAgentCommissionPct(BigDecimal.ZERO)
                .buyerAgentCommissionPct(BigDecimal.ZERO)
                .property(null)
                .contractType(ContractType.SALE)
                .status(ContractStatus.DRAFT)
                .build();

        ContractResponse dto = mapper.toResponse(contract);

        assertThat(dto.propertyId()).isNull();
        assertThat(dto.propertyTitle()).isNull();
    }

    @Test
    @DisplayName("toResponse_nullBuyerAndSeller_returnsNullPartyFields")
    void toResponse_nullBuyerAndSeller_returnsNullPartyFields() {
        Contract contract = Contract.builder()
                .amount(BigDecimal.TEN)
                .commissionPct(BigDecimal.ZERO)
                .listingAgentCommissionPct(BigDecimal.ZERO)
                .buyerAgentCommissionPct(BigDecimal.ZERO)
                .buyer(null)
                .seller(null)
                .contractType(ContractType.SALE)
                .status(ContractStatus.DRAFT)
                .build();

        ContractResponse dto = mapper.toResponse(contract);

        assertThat(dto.buyerId()).isNull();
        assertThat(dto.buyerName()).isNull();
        assertThat(dto.buyerEmail()).isNull();
        assertThat(dto.sellerId()).isNull();
        assertThat(dto.sellerName()).isNull();
        assertThat(dto.sellerEmail()).isNull();
    }

    @Test
    @DisplayName("toResponse_nullAgents_returnsNullAgentFields")
    void toResponse_nullAgents_returnsNullAgentFields() {
        Contract contract = Contract.builder()
                .amount(BigDecimal.TEN)
                .commissionPct(BigDecimal.ZERO)
                .listingAgentCommissionPct(BigDecimal.ZERO)
                .buyerAgentCommissionPct(BigDecimal.ZERO)
                .listingAgent(null)
                .buyerAgent(null)
                .contractType(ContractType.SALE)
                .status(ContractStatus.DRAFT)
                .build();

        ContractResponse dto = mapper.toResponse(contract);

        assertThat(dto.listingAgentId()).isNull();
        assertThat(dto.listingAgentName()).isNull();
        assertThat(dto.buyerAgentId()).isNull();
        assertThat(dto.buyerAgentName()).isNull();
    }

    @Test
    @DisplayName("toResponse_withFullParties_mapsAllPartyFields")
    void toResponse_withFullParties_mapsAllPartyFields() {
        Property property = mock(Property.class);
        when(property.getId()).thenReturn(10L);
        when(property.getTitle()).thenReturn("Residencia Norte");

        User buyer = mock(User.class);
        when(buyer.getId()).thenReturn(1L);
        when(buyer.getName()).thenReturn("Carlos");
        when(buyer.getEmail()).thenReturn("carlos@mail.com");

        User seller = mock(User.class);
        when(seller.getId()).thenReturn(2L);
        when(seller.getName()).thenReturn("Maria");
        when(seller.getEmail()).thenReturn("maria@mail.com");

        User agentUser = mock(User.class);
        when(agentUser.getName()).thenReturn("Agente Pro");

        AgentProfile listingAgent = mock(AgentProfile.class);
        when(listingAgent.getId()).thenReturn(5L);
        when(listingAgent.getUser()).thenReturn(agentUser);

        Contract contract = Contract.builder()
                .property(property)
                .buyer(buyer)
                .seller(seller)
                .listingAgent(listingAgent)
                .buyerAgent(null)
                .amount(new BigDecimal("800000"))
                .commissionPct(new BigDecimal("3.00"))
                .listingAgentCommissionPct(new BigDecimal("3.00"))
                .buyerAgentCommissionPct(BigDecimal.ZERO)
                .contractType(ContractType.SALE)
                .status(ContractStatus.SIGNED)
                .startDate(LocalDate.of(2025, 1, 15))
                .build();

        ContractResponse dto = mapper.toResponse(contract);

        assertThat(dto.propertyId()).isEqualTo(10L);
        assertThat(dto.propertyTitle()).isEqualTo("Residencia Norte");
        assertThat(dto.buyerId()).isEqualTo(1L);
        assertThat(dto.buyerName()).isEqualTo("Carlos");
        assertThat(dto.buyerEmail()).isEqualTo("carlos@mail.com");
        assertThat(dto.sellerId()).isEqualTo(2L);
        assertThat(dto.sellerName()).isEqualTo("Maria");
        assertThat(dto.listingAgentId()).isEqualTo(5L);
        assertThat(dto.listingAgentName()).isEqualTo("Agente Pro");
        assertThat(dto.buyerAgentId()).isNull();
        assertThat(dto.buyerAgentName()).isNull();
        assertThat(dto.contractType()).isEqualTo(ContractType.SALE);
        assertThat(dto.status()).isEqualTo(ContractStatus.SIGNED);
    }

    // ─── toSummaryResponse ───────────────────────────────────────────────────

    @Test
    @DisplayName("toSummaryResponse_hasSigned_true_mapsFlag")
    void toSummaryResponse_hasSigned_true_mapsFlag() {
        Contract contract = Contract.builder()
                .amount(new BigDecimal("500000"))
                .commissionPct(new BigDecimal("4.00"))
                .listingAgentCommissionPct(new BigDecimal("4.00"))
                .buyerAgentCommissionPct(BigDecimal.ZERO)
                .contractType(ContractType.RENT)
                .status(ContractStatus.SIGNED)
                .startDate(LocalDate.of(2025, 3, 1))
                .build();

        ContractSummaryResponse dto = mapper.toSummaryResponse(contract, true);

        assertThat(dto.currentUserHasSigned()).isTrue();
        assertThat(dto.amount()).isEqualByComparingTo("500000");
        // totalAmt = 500_000 * 4 / 100 = 20_000
        assertThat(dto.totalCommissionAmount()).isEqualByComparingTo("20000.00");
        assertThat(dto.status()).isEqualTo(ContractStatus.SIGNED);
    }

    @Test
    @DisplayName("toSummaryResponse_hasSigned_false_mapsFlag")
    void toSummaryResponse_hasSigned_false_mapsFlag() {
        Contract contract = Contract.builder()
                .amount(BigDecimal.TEN)
                .commissionPct(BigDecimal.ZERO)
                .listingAgentCommissionPct(BigDecimal.ZERO)
                .buyerAgentCommissionPct(BigDecimal.ZERO)
                .contractType(ContractType.SALE)
                .status(ContractStatus.DRAFT)
                .build();

        ContractSummaryResponse dto = mapper.toSummaryResponse(contract, false);

        assertThat(dto.currentUserHasSigned()).isFalse();
    }

    @Test
    @DisplayName("toSummaryResponse_nullProperty_returnsNullPropertyFields")
    void toSummaryResponse_nullProperty_returnsNullPropertyFields() {
        Contract contract = Contract.builder()
                .amount(BigDecimal.TEN)
                .commissionPct(BigDecimal.ZERO)
                .listingAgentCommissionPct(BigDecimal.ZERO)
                .buyerAgentCommissionPct(BigDecimal.ZERO)
                .property(null)
                .contractType(ContractType.SALE)
                .status(ContractStatus.DRAFT)
                .build();

        ContractSummaryResponse dto = mapper.toSummaryResponse(contract, false);

        assertThat(dto.propertyId()).isNull();
        assertThat(dto.propertyTitle()).isNull();
    }

    @Test
    @DisplayName("toSummaryResponse_agentWithNullUser_returnsNullAgentName")
    void toSummaryResponse_agentWithNullUser_returnsNullAgentName() {
        AgentProfile agentNoUser = mock(AgentProfile.class);
        when(agentNoUser.getUser()).thenReturn(null);

        Contract contract = Contract.builder()
                .amount(BigDecimal.TEN)
                .commissionPct(BigDecimal.ZERO)
                .listingAgentCommissionPct(BigDecimal.ZERO)
                .buyerAgentCommissionPct(BigDecimal.ZERO)
                .listingAgent(agentNoUser)
                .contractType(ContractType.SALE)
                .status(ContractStatus.DRAFT)
                .build();

        ContractSummaryResponse dto = mapper.toSummaryResponse(contract, false);

        assertThat(dto.listingAgentName()).isNull();
    }
}
