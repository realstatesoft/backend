package com.openroof.openroof.service;

import com.openroof.openroof.dto.dashboard.*;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.agent.AgentProfile;
import com.openroof.openroof.model.contract.Contract;
import com.openroof.openroof.model.enums.ContractStatus;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.PropertyType;
import com.openroof.openroof.model.enums.VisitRequestStatus;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

        private final AgentClientRepository agentClientRepository;
        private final AgentProfileRepository agentProfileRepository;
        private final PropertyRepository propertyRepository;
        private final VisitRequestRepository visitRequestRepository;
        private final ContractRepository contractRepository;
        private final PropertyViewRepository propertyViewRepository;
        private final OfferRepository offerRepository;
        private final UserRepository userRepository;

        // ─── Agent Dashboard Stats ────────────────────────────────────────────────

        public AgentDashboardStatsResponse getAgentStats(String email) {
                User user = findUserByEmail(email);
                AgentProfile agent = findAgentByUserId(user.getId());
                Long agentId = agent.getId();

                long activeClients = agentClientRepository.countByAgent_Id(agentId);
                long totalSales = propertyRepository.countByAgentIdAndStatus(agentId, PropertyStatus.SOLD);
                long scheduledVisits = visitRequestRepository.countByAgentIdAndStatus(agentId,
                                VisitRequestStatus.PENDING)
                                + visitRequestRepository.countByAgentIdAndStatus(agentId, VisitRequestStatus.ACCEPTED);

                List<Contract> signedContracts = contractRepository.findBySeller_Id(user.getId()).stream()
                                .filter(c -> c.getStatus() == ContractStatus.SIGNED)
                                .collect(Collectors.toList());
                BigDecimal commissions = signedContracts.stream()
                                .map(c -> c.getAmount().multiply(BigDecimal.valueOf(0.03)))
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return new AgentDashboardStatsResponse(
                                CountStatItem.of(activeClients, 0),
                                CountStatItem.of(totalSales, 0),
                                CountStatItem.of(scheduledVisits, 0),
                                MoneyStatItem.of(roundCommission(commissions), 0));
        }

        // ─── Owner Dashboard Stats ────────────────────────────────────────────────

        public OwnerDashboardStatsResponse getOwnerStats(String email) {
                User user = findUserByEmail(email);
                Long ownerId = user.getId();

                long myProperties = propertyRepository.countActiveByOwnerId(ownerId);
                long totalVisits = visitRequestRepository.countByPropertyOwnerId(ownerId);
                long inquiries = offerRepository.countByPropertyOwnerId(ownerId);
                long views = propertyViewRepository.countByPropertyOwnerId(ownerId);

                return new OwnerDashboardStatsResponse(
                                CountStatItem.of(myProperties, 0),
                                CountStatItem.of(totalVisits, 0),
                                CountStatItem.of(inquiries, 0),
                                CountStatItem.of(views, 0));
        }

        // ─── Sales List (from Contracts) ──────────────────────────────────────────

        public List<SaleItemResponse> getSales(String email) {
                User user = findUserByEmail(email);

                return contractRepository.findBySeller_Id(user.getId()).stream()
                                .map(c -> new SaleItemResponse(
                                                c.getId(),
                                                c.getProperty() != null ? c.getProperty().getTitle() : "N/A",
                                                c.getBuyer() != null ? c.getBuyer().getName() : "N/A",
                                                c.getAmount(),
                                                roundCommission(c.getAmount().multiply(BigDecimal.valueOf(0.03))),
                                                c.getStartDate(),
                                                c.getStatus().name().toLowerCase()))
                                .collect(Collectors.toList());
        }

        // ─── Sales Summary ────────────────────────────────────────────────────────

        public SalesSummaryResponse getSalesSummary(String email) {
                User user = findUserByEmail(email);

                List<Contract> contracts = contractRepository.findBySeller_Id(user.getId());

                BigDecimal totalSoldDecimal = contracts.stream()
                                .filter(c -> c.getStatus() == ContractStatus.SIGNED)
                                .map(Contract::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                long totalSold = totalSoldDecimal.setScale(0, RoundingMode.HALF_UP).longValue();

                long monthlyCommissions = roundCommission(totalSoldDecimal.multiply(BigDecimal.valueOf(0.03)))
                                .longValue();

                int activeContracts = (int) contracts.stream()
                                .filter(c -> c.getStatus() == ContractStatus.DRAFT
                                                || c.getStatus() == ContractStatus.SENT
                                                || c.getStatus() == ContractStatus.PARTIALLY_SIGNED)
                                .count();

                // Build monthly data for last 6 months
                List<SalesSummaryResponse.MonthlyDataPoint> monthlyData = new ArrayList<>();
                LocalDate now = LocalDate.now();
                for (int i = 5; i >= 0; i--) {
                        LocalDate month = now.minusMonths(i);
                        String monthName = month.getMonth().getDisplayName(TextStyle.SHORT,
                                        Locale.forLanguageTag("es"));
                        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);

                        long monthlySales = contracts.stream()
                                        .filter(c -> c.getStatus() == ContractStatus.SIGNED && c.getStartDate() != null)
                                        .filter(c -> c.getStartDate().getMonth() == month.getMonth()
                                                        && c.getStartDate().getYear() == month.getYear())
                                        .map(Contract::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                        .setScale(0, RoundingMode.HALF_UP)
                                        .longValue();

                        monthlyData.add(new SalesSummaryResponse.MonthlyDataPoint(monthName, monthlySales));
                }

                return new SalesSummaryResponse(totalSold, monthlyCommissions, activeContracts, monthlyData);
        }

        // ─── Reports Summary ──────────────────────────────────────────────────────

        public ReportsSummaryResponse getReportsSummary() {
                long totalProperties = propertyRepository.count();
                long publishedCount = propertyRepository.countByStatus(PropertyStatus.PUBLISHED);
                long soldCount = propertyRepository.countByStatus(PropertyStatus.SOLD);

                int closingRate = totalProperties > 0
                                ? (int) ((soldCount * 100) / totalProperties)
                                : 0;

                var marketMetrics = new ReportsSummaryResponse.MarketMetrics(
                                0, 0, publishedCount, 0, 0, 0, closingRate, 0);

                // Property distribution by type
                List<ReportsSummaryResponse.TypeDistribution> byType = new ArrayList<>();
                for (PropertyType type : PropertyType.values()) {
                        long count = propertyRepository.countByPropertyTypeAndStatus(type, PropertyStatus.PUBLISHED);
                        if (count > 0) {
                                byType.add(new ReportsSummaryResponse.TypeDistribution(type.name(), (int) count));
                        }
                }

                // Monthly trend placeholder
                List<ReportsSummaryResponse.MonthlyTrend> monthlyTrend = new ArrayList<>();
                LocalDate now = LocalDate.now();
                for (int i = 5; i >= 0; i--) {
                        LocalDate month = now.minusMonths(i);
                        String monthName = month.getMonth().getDisplayName(TextStyle.SHORT,
                                        Locale.forLanguageTag("es"));
                        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                        monthlyTrend.add(new ReportsSummaryResponse.MonthlyTrend(monthName, 0, 0));
                }

                return new ReportsSummaryResponse(marketMetrics, byType, monthlyTrend);
        }
        // ─── Sales Performance (Year vs Year comparison) ─────────────────────────

        public List<MonthlySalesData> getSalesPerformance(String email) {
                User user = findUserByEmail(email);
                int currentYear = LocalDate.now().getYear();
                int previousYear = currentYear - 1;

                List<RawSalesData> raw = contractRepository.findMonthlySalesGrouped(
                                user.getId(),
                                List.of(PropertyStatus.SOLD, PropertyStatus.RENTED),
                                currentYear,
                                previousYear);

                Map<String, RawSalesData> index = raw.stream()
                                .collect(Collectors.toMap(
                                                r -> r.year() + "-" + r.month(),
                                                r -> r));

                List<MonthlySalesData> result = new ArrayList<>();
                for (int month = 1; month <= 12; month++) {
                        RawSalesData curr = index.get(currentYear + "-" + month);
                        RawSalesData prev = index.get(previousYear + "-" + month);

                        result.add(new MonthlySalesData(
                                        month,
                                        curr != null ? new YearData(curr.totalAmount(), curr.count())
                                                        : YearData.zero(),
                                        prev != null ? new YearData(prev.totalAmount(), prev.count())
                                                        : YearData.zero()));
                }
                return result;
        }

        // ─── Helpers ──────────────────────────────────────────────────────────────

        private User findUserByEmail(String email) {
                return userRepository.findByEmail(email)
                                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        }

        private AgentProfile findAgentByUserId(Long userId) {
                return agentProfileRepository.findByUser_Id(userId)
                                .orElseThrow(() -> new ResourceNotFoundException("Perfil de agente no encontrado"));
        }

        private BigDecimal roundCommission(BigDecimal amount) {
                return amount.setScale(0, RoundingMode.HALF_UP);
        }
}
