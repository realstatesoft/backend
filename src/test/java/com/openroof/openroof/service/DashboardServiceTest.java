package com.openroof.openroof.service;

import com.openroof.openroof.dto.dashboard.*;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.ContractType;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.VisitRequestStatus;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private AgentClientRepository agentClientRepository;
    @Mock private AgentProfileRepository agentProfileRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private VisitRequestRepository visitRequestRepository;
    @Mock private ContractRepository contractRepository;
    @Mock private PropertyViewRepository propertyViewRepository;
    @Mock private OfferRepository offerRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;
    private AgentProfile testAgent;
    private String testEmail = "agent@test.com";

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email(testEmail)
                .name("Test Agent")
                .build();
        testUser.setId(1L);

        testAgent = AgentProfile.builder()
                .user(testUser)
                .build();
        testAgent.setId(10L);
    }

    @Nested
    @DisplayName("getAgentStats()")
    class GetAgentStatsTests {

        @Test
        @DisplayName("Obtener estadísticas de agente → retorna valores correctos")
        void getAgentStats_returnsCorrectValues() {
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testAgent));
            
            when(agentClientRepository.countByAgent_Id(10L)).thenReturn(5L);
            when(propertyRepository.countByAgentIdAndStatus(10L, PropertyStatus.SOLD)).thenReturn(3L);
            when(visitRequestRepository.countByAgentIdAndStatus(10L, VisitRequestStatus.PENDING)).thenReturn(2L);
            when(visitRequestRepository.countByAgentIdAndStatus(10L, VisitRequestStatus.ACCEPTED)).thenReturn(1L);

            // listingAgent = testAgent so computeMyCommission uses listingAgentCommissionPct (3% por @Builder.Default)
            Contract contract = Contract.builder()
                    .amount(new BigDecimal("100000"))
                    .status(ContractStatus.SIGNED)
                    .listingAgent(testAgent)
                    .build();
            when(contractRepository.findAllByParticipant(1L, 10L)).thenReturn(List.of(contract));

            AgentDashboardStatsResponse stats = dashboardService.getAgentStats(testEmail);

            assertThat(stats).isNotNull();
            assertThat(stats.activeClients().value()).isEqualTo(5L);
            assertThat(stats.totalSales().value()).isEqualTo(3L);
            assertThat(stats.scheduledVisits().value()).isEqualTo(3L); // 2 pending + 1 accepted
            assertThat(stats.commissions().value()).isEqualTo(new BigDecimal("3000")); // 3% de 100000 (listingAgentCommissionPct @Builder.Default)
        }

        @Test
        @DisplayName("Usuario no encontrado → lanza ResourceNotFoundException")
        void userNotFound_throwsException() {
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dashboardService.getAgentStats(testEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    @Nested
    @DisplayName("getOwnerStats()")
    class GetOwnerStatsTests {
        @Test
        @DisplayName("Obtener estadísticas de propietario → retorna valores correctos")
        void getOwnerStats_returnsCorrectValues() {
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            
            when(propertyRepository.countActiveByOwnerId(1L)).thenReturn(2L);
            when(visitRequestRepository.countByPropertyOwnerId(1L)).thenReturn(10L);
            when(offerRepository.countByPropertyOwnerId(1L)).thenReturn(4L);
            when(propertyViewRepository.countByPropertyOwnerId(1L)).thenReturn(100L);

            OwnerDashboardStatsResponse stats = dashboardService.getOwnerStats(testEmail);

            assertThat(stats).isNotNull();
            assertThat(stats.myProperties().value()).isEqualTo(2L);
            assertThat(stats.totalVisits().value()).isEqualTo(10L);
            assertThat(stats.inquiries().value()).isEqualTo(4L);
            assertThat(stats.views().value()).isEqualTo(100L);
        }
    }

    @Nested
    @DisplayName("getSalesSummary()")
    class GetSalesSummaryTests {
        @Test
        @DisplayName("Obtener resumen de ventas → calcula totales y datos mensuales")
        void getSalesSummary_returnsCorrectAggregates() {
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testAgent));

            // listingAgent = testAgent para que computeMyCommission calcule 3% = 3000
            Contract c1 = Contract.builder()
                    .amount(new BigDecimal("100000"))
                    .status(ContractStatus.SIGNED)
                    .startDate(java.time.LocalDate.now())
                    .listingAgent(testAgent)
                    .build();
            Contract c2 = Contract.builder()
                    .amount(new BigDecimal("200000"))
                    .status(ContractStatus.DRAFT)
                    .build();

            when(contractRepository.findAllByParticipant(1L, 10L)).thenReturn(List.of(c1, c2));

            SalesSummaryResponse summary = dashboardService.getSalesSummary(testEmail);

            assertThat(summary).isNotNull();
            assertThat(summary.totalSold()).isEqualTo(100000L);
            assertThat(summary.totalCommissions()).isEqualTo(3000L); // 3% de 100000 (listingAgentCommissionPct @Builder.Default)
            assertThat(summary.signedContracts()).isEqualTo(1);
            assertThat(summary.activeContracts()).isEqualTo(1); // c2 en DRAFT
            assertThat(summary.monthlyData()).hasSize(6);
            
            // Check current month data
            String currentMonth = java.time.LocalDate.now().getMonth()
                    .getDisplayName(java.time.format.TextStyle.SHORT, java.util.Locale.of("es"));
            currentMonth = currentMonth.substring(0, 1).toUpperCase() + currentMonth.substring(1);
            
            String finalCurrentMonth = currentMonth;
            assertThat(summary.monthlyData().stream()
                    .filter(d -> d.month().equals(finalCurrentMonth))
                    .findFirst()
                    .get().sales()).isEqualTo(100000L);
        }
    }

    @Nested
    @DisplayName("getSales()")
    class GetSalesTests {

        @Test
        @DisplayName("Retorna lista de contratos con comisión calculada al 3%")
        void getSales_returnsContractsWithCommission() {
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testAgent));

            Property property = new Property();
            property.setTitle("Casa en Las Mercedes");

            User buyer = User.builder().name("Juan Pérez").build();
            User seller = User.builder().name("Propietario Test").build();

            // listingAgent = testAgent → myCommission = listingAgentCommissionPct (3% @Builder.Default)
            Contract signed = Contract.builder()
                    .amount(new BigDecimal("250000"))
                    .status(ContractStatus.SIGNED)
                    .contractType(ContractType.SALE)
                    .startDate(LocalDate.of(2026, 1, 15))
                    .property(property)
                    .buyer(buyer)
                    .seller(seller)
                    .listingAgent(testAgent)
                    .build();

            Contract draft = Contract.builder()
                    .amount(new BigDecimal("180000"))
                    .status(ContractStatus.DRAFT)
                    .contractType(ContractType.RENT)
                    .property(property)
                    .buyer(buyer)
                    .seller(seller)
                    .listingAgent(testAgent)
                    .build();

            when(contractRepository.findAllByParticipant(1L, 10L)).thenReturn(List.of(signed, draft));

            List<SaleItemResponse> sales = dashboardService.getSales(testEmail);

            assertThat(sales).hasSize(2);

            SaleItemResponse first = sales.get(0);
            assertThat(first.amount()).isEqualByComparingTo(new BigDecimal("250000"));
            assertThat(first.totalCommission()).isEqualByComparingTo(new BigDecimal("7500")); // 3% de 250000
            assertThat(first.myCommission()).isEqualByComparingTo(new BigDecimal("7500")); // listing agent 3%
            assertThat(first.myRole()).isEqualTo("LISTING_AGENT");
            assertThat(first.status()).isEqualTo("SIGNED");
            assertThat(first.property()).isEqualTo("Casa en Las Mercedes");
            assertThat(first.buyer()).isEqualTo("Juan Pérez");

            SaleItemResponse second = sales.get(1);
            assertThat(second.totalCommission()).isEqualByComparingTo(new BigDecimal("5400")); // 3% de 180000
            assertThat(second.status()).isEqualTo("DRAFT");
        }

        @Test
        @DisplayName("Sin contratos → retorna lista vacía")
        void getSales_noContracts_returnsEmptyList() {
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testAgent));
            when(contractRepository.findAllByParticipant(1L, 10L)).thenReturn(List.of());

            List<SaleItemResponse> sales = dashboardService.getSales(testEmail);

            assertThat(sales).isEmpty();
        }

        @Test
        @DisplayName("Propiedad o comprador nulos → usa 'N/A' como fallback")
        void getSales_nullPropertyAndBuyer_usesNAFallback() {
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
            when(agentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testAgent));

            Contract contract = Contract.builder()
                    .amount(new BigDecimal("100000"))
                    .status(ContractStatus.DRAFT)
                    .contractType(ContractType.SALE) // requerido: service llama .name() sobre contractType
                    .property(null)
                    .buyer(null)
                    .seller(null)
                    .build();

            when(contractRepository.findAllByParticipant(1L, 10L)).thenReturn(List.of(contract));

            List<SaleItemResponse> sales = dashboardService.getSales(testEmail);

            assertThat(sales).hasSize(1);
            assertThat(sales.get(0).property()).isEqualTo("N/A");
            assertThat(sales.get(0).buyer()).isEqualTo("N/A");   // campo renombrado: client() → buyer()
            assertThat(sales.get(0).seller()).isEqualTo("N/A");
        }

            @Test
            @DisplayName("commission_pct nulo → lanza IllegalArgumentException")
            void getSales_nullCommissionPct_throwsException() {
                when(userRepository.findByEmail(testEmail)).thenReturn(Optional.of(testUser));
                when(agentProfileRepository.findByUser_Id(1L)).thenReturn(Optional.of(testAgent));

                Contract contract = Contract.builder()
                    .amount(new BigDecimal("100000"))
                    .status(ContractStatus.DRAFT)
                    .contractType(ContractType.SALE)
                    .listingAgent(testAgent)
                    .commissionPct(null)
                    .build();

                when(contractRepository.findAllByParticipant(1L, 10L)).thenReturn(List.of(contract));

                assertThatThrownBy(() -> dashboardService.getSales(testEmail))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("commission_pct nulo");
            }

        @Test
        @DisplayName("Usuario no encontrado → lanza ResourceNotFoundException")
        void getSales_userNotFound_throwsException() {
            when(userRepository.findByEmail(testEmail)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> dashboardService.getSales(testEmail))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuario no encontrado");
        }
    }

    @Nested
    @DisplayName("getReportsSummary()")
    class GetReportsSummaryTests {
        @Test
        @DisplayName("Obtener resumen de reportes → calcula tasas de mercado")
        void getReportsSummary_calculatesClosingRate() {
            when(propertyRepository.count()).thenReturn(10L);
            when(propertyRepository.countByStatus(PropertyStatus.PUBLISHED)).thenReturn(7L);
            when(propertyRepository.countByStatus(PropertyStatus.SOLD)).thenReturn(3L);

            ReportsSummaryResponse report = dashboardService.getReportsSummary();

            assertThat(report).isNotNull();
            assertThat(report.marketMetrics().closingRate()).isEqualTo(30); // (3*100)/10
            assertThat(report.marketMetrics().propertiesListed()).isEqualTo(7L);
            assertThat(report.monthlyTrend()).hasSize(6);
        }
    }
}
