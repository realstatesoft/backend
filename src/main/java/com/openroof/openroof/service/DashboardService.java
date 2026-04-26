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
import com.openroof.openroof.mapper.ContractMapper;
import com.openroof.openroof.mapper.PropertyMapper;
import com.openroof.openroof.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

        private final PropertyMapper propertyMapper;
        private final ContractMapper contractMapper;
        private final VisitRequestService visitRequestService;

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

                List<Contract> signedContracts = contractRepository
                                .findAllByParticipant(user.getId(), agentId).stream()
                                .filter(c -> c.getStatus() == ContractStatus.SIGNED)
                                .collect(Collectors.toList());
                BigDecimal commissions = signedContracts.stream()
                                .map(c -> computeMyCommission(c, user.getId(), agentId))
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
                BigDecimal earnings = contractRepository.sumAmountBySellerIdSigned(ownerId);

                return new OwnerDashboardStatsResponse(
                                CountStatItem.of(myProperties, 0),
                                CountStatItem.of(totalVisits, 0),
                                CountStatItem.of(inquiries, 0),
                                CountStatItem.of(views, 0),
                                MoneyStatItem.of(earnings, 0));
        }

        public OwnerDashboardOverviewResponse getOwnerOverview(String email) {
                User user = findUserByEmail(email);
                Long userId = user.getId();

                // 1. Estadísticas
                OwnerDashboardStatsResponse stats = getOwnerStats(email);

                // 2. Propiedades recientes (últimas 5)
                var pageable = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"));
                List<com.openroof.openroof.dto.property.PropertySummaryResponse> recentProperties = propertyRepository
                                .findByOwner_IdAndTrashedAtIsNull(userId, pageable)
                                .map(propertyMapper::toSummaryResponse)
                                .getContent();

                // 3. Contratos urgentes (pendientes de firma por el usuario)
                List<com.openroof.openroof.dto.contract.ContractSummaryResponse> urgentContracts = contractRepository
                                .findPendingSignaturesForUser(userId).stream()
                                .map(c -> contractMapper.toSummaryResponse(c, false))
                                .collect(Collectors.toList());
                
                // 4. Solicitudes de visita nuevas/pendientes
                List<com.openroof.openroof.dto.visit.VisitRequestResponse> pendingVisits = visitRequestService
                                .getMyRequestsAsOwner(email).stream()
                                .filter(v -> v.status() == VisitRequestStatus.PENDING)
                                .limit(5)
                                .collect(Collectors.toList());

                return new OwnerDashboardOverviewResponse(stats, recentProperties, urgentContracts, pendingVisits);
        }

        // ─── Sales List (from Contracts) ──────────────────────────────────────────

        public List<SaleItemResponse> getSales(String email) {
                User user = findUserByEmail(email);
                Long agentProfileId = agentProfileRepository.findByUser_Id(user.getId())
                                .map(AgentProfile::getId).orElse(-1L);

                return contractRepository.findAllByParticipant(user.getId(), agentProfileId).stream()
                                .map(c -> {
                                        String myRole = resolveRole(c, user.getId(), agentProfileId);
                                        BigDecimal myComm = computeMyCommission(c, user.getId(), agentProfileId);
                                        if (c.getCommissionPct() == null) {
                                                throw new IllegalArgumentException(
                                                                "Contrato " + c.getId() + " tiene commission_pct nulo");
                                        }
                                        BigDecimal totalComm = roundCommission(c.getAmount().multiply(
                                                        commissionPct(c.getCommissionPct())));
                                        return new SaleItemResponse(
                                                        c.getId(),
                                                        c.getProperty() != null ? c.getProperty().getTitle() : "N/A",
                                                        c.getBuyer() != null ? c.getBuyer().getName() : "N/A",
                                                        c.getSeller() != null ? c.getSeller().getName() : "N/A",
                                                        c.getContractType().name(),
                                                        c.getAmount(),
                                                        totalComm,
                                                        myComm,
                                                        myRole,
                                                        c.getStartDate(),
                                                        c.getStatus().name());
                                })
                                .collect(Collectors.toList());
        }

        // ─── Sales Summary ────────────────────────────────────────────────────────

        public SalesSummaryResponse getSalesSummary(String email) {
                User user = findUserByEmail(email);
                Long agentProfileId = agentProfileRepository.findByUser_Id(user.getId())
                                .map(AgentProfile::getId).orElse(-1L);

                List<Contract> contracts = contractRepository.findAllByParticipant(user.getId(), agentProfileId);

                List<Contract> signed = contracts.stream()
                                .filter(c -> c.getStatus() == ContractStatus.SIGNED)
                                .collect(Collectors.toList());

                long totalSold = signed.stream()
                                .map(Contract::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(0, RoundingMode.HALF_UP).longValue();

                long myCommissions = signed.stream()
                                .map(c -> computeMyCommission(c, user.getId(), agentProfileId))
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(0, RoundingMode.HALF_UP).longValue();

                int signedCount = signed.size();

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

                        final LocalDate m = month;
                        List<Contract> monthSigned = signed.stream()
                                        .filter(c -> c.getStartDate() != null
                                                        && c.getStartDate().getMonth() == m.getMonth()
                                                        && c.getStartDate().getYear() == m.getYear())
                                        .collect(Collectors.toList());

                        long monthlySales = monthSigned.stream()
                                        .map(Contract::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                        .setScale(0, RoundingMode.HALF_UP).longValue();

                        long monthlyComm = monthSigned.stream()
                                        .map(c -> computeMyCommission(c, user.getId(), agentProfileId))
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                                        .setScale(0, RoundingMode.HALF_UP).longValue();

                        monthlyData.add(new SalesSummaryResponse.MonthlyDataPoint(monthName, monthlySales, monthlyComm));
                }

                return new SalesSummaryResponse(totalSold, myCommissions, signedCount, activeContracts, monthlyData);
        }

        // ─── Reports Summary ──────────────────────────────────────────────────────

        public ReportsSummaryResponse getReportsSummary() {
                long totalProperties = propertyRepository.count();
                long publishedCount = propertyRepository.countByStatus(PropertyStatus.PUBLISHED);
                long soldCount = propertyRepository.countByStatus(PropertyStatus.SOLD);

                int closingRate = totalProperties > 0
                                ? (int) ((soldCount * 100) / totalProperties)
                                : 0;

                Double avgPriceRaw = propertyRepository.findAvgPriceByStatuses(
                                List.of(PropertyStatus.PUBLISHED, PropertyStatus.SOLD));
                long avgPrice = (avgPriceRaw != null && !avgPriceRaw.isNaN())
                                ? BigDecimal.valueOf(avgPriceRaw).setScale(0, RoundingMode.HALF_UP).longValue()
                                : 0;

                var marketMetrics = new ReportsSummaryResponse.MarketMetrics(
                                avgPrice, 0, publishedCount, 0, 0, 0, closingRate, 0);

                // Property distribution by type
                List<ReportsSummaryResponse.TypeDistribution> byType = new ArrayList<>();
                for (PropertyType type : PropertyType.values()) {
                        long count = propertyRepository.countByPropertyTypeAndStatus(type, PropertyStatus.PUBLISHED);
                        if (count > 0) {
                                byType.add(new ReportsSummaryResponse.TypeDistribution(type.name(), (int) count));
                        }
                }

                // Monthly trend — real data for last 6 months
                List<ReportsSummaryResponse.MonthlyTrend> monthlyTrend = new ArrayList<>();
                LocalDate now = LocalDate.now();
                List<VisitRequestStatus> activeVisitStatuses = List.of(
                                VisitRequestStatus.PENDING,
                                VisitRequestStatus.COUNTER_PROPOSED,
                                VisitRequestStatus.ACCEPTED);
                for (int i = 5; i >= 0; i--) {
                        LocalDate month = now.minusMonths(i);
                        String monthName = month.getMonth().getDisplayName(TextStyle.SHORT,
                                        Locale.forLanguageTag("es"));
                        monthName = monthName.substring(0, 1).toUpperCase() + monthName.substring(1);
                        int year = month.getYear();
                        int monthValue = month.getMonthValue();
                        long ventas = contractRepository.countSignedByYearAndMonth(year, monthValue);
                        long visitas = visitRequestRepository.countByStatusesAndYearAndMonth(
                                        activeVisitStatuses, year, monthValue);
                        monthlyTrend.add(new ReportsSummaryResponse.MonthlyTrend(monthName, Math.toIntExact(ventas), Math.toIntExact(visitas)));
                }

                return new ReportsSummaryResponse(marketMetrics, byType, monthlyTrend);
        }

        // ─── Sales Performance (Year vs Year comparison) ─────────────────────────

        public List<MonthlySalesData> getSalesPerformance(String email) {
                User user = findUserByEmail(email);
                Long agentProfileId = agentProfileRepository.findByUser_Id(user.getId())
                                .map(AgentProfile::getId).orElse(-1L);

                int currentYear = LocalDate.now().getYear();
                int previousYear = currentYear - 1;

                List<Contract> signed = contractRepository
                                .findAllByParticipant(user.getId(), agentProfileId).stream()
                                .filter(c -> c.getStatus() == ContractStatus.SIGNED
                                                && c.getStartDate() != null
                                                && (c.getStartDate().getYear() == currentYear
                                                                || c.getStartDate().getYear() == previousYear))
                                .collect(Collectors.toList());

                List<MonthlySalesData> result = new ArrayList<>();
                for (int month = 1; month <= 12; month++) {
                        final int m = month;

                        List<Contract> curr = signed.stream()
                                        .filter(c -> c.getStartDate().getYear() == currentYear
                                                        && c.getStartDate().getMonthValue() == m)
                                        .collect(Collectors.toList());

                        List<Contract> prev = signed.stream()
                                        .filter(c -> c.getStartDate().getYear() == previousYear
                                                        && c.getStartDate().getMonthValue() == m)
                                        .collect(Collectors.toList());

                        result.add(new MonthlySalesData(
                                        month,
                                        curr.isEmpty() ? YearData.zero()
                                                        : new YearData(
                                                                        curr.stream().map(Contract::getAmount)
                                                                                        .reduce(BigDecimal.ZERO,
                                                                                                        BigDecimal::add),
                                                                        curr.size()),
                                        prev.isEmpty() ? YearData.zero()
                                                        : new YearData(
                                                                        prev.stream().map(Contract::getAmount)
                                                                                        .reduce(BigDecimal.ZERO,
                                                                                                        BigDecimal::add),
                                                                        prev.size())));
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

        private BigDecimal commissionPct(BigDecimal pct) {
                if (pct == null) {
                        throw new IllegalArgumentException("commission_pct es nulo; datos de comisión incompletos");
                }
                return pct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        }

        private BigDecimal computeMyCommission(Contract c, Long userId, Long agentProfileId) {
                BigDecimal pct = BigDecimal.ZERO;
                if (c.getListingAgent() != null && c.getListingAgent().getId().equals(agentProfileId)) {
                        pct = c.getListingAgentCommissionPct() != null ? c.getListingAgentCommissionPct()
                                        : BigDecimal.ZERO;
                } else if (c.getBuyerAgent() != null && c.getBuyerAgent().getId().equals(agentProfileId)) {
                        pct = c.getBuyerAgentCommissionPct() != null ? c.getBuyerAgentCommissionPct()
                                        : BigDecimal.ZERO;
                }
                return roundCommission(c.getAmount().multiply(
                                pct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)));
        }

        private String resolveRole(Contract c, Long userId, Long agentProfileId) {
                if (c.getListingAgent() != null && c.getListingAgent().getId().equals(agentProfileId))
                        return "LISTING_AGENT";
                if (c.getBuyerAgent() != null && c.getBuyerAgent().getId().equals(agentProfileId))
                        return "BUYER_AGENT";
                if (c.getSeller() != null && c.getSeller().getId().equals(userId))
                        return "SELLER";
                if (c.getBuyer() != null && c.getBuyer().getId().equals(userId))
                        return "BUYER";
                return "PARTICIPANT";
        }
}
