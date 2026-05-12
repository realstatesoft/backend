package com.openroof.openroof.service;

import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.enums.LedgerEntryCategory;
import com.openroof.openroof.model.enums.LedgerEntryType;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.ledger.LedgerEntry;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RecurringCharge;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.repository.LedgerEntryRepository;
import com.openroof.openroof.repository.RecurringChargeRepository;
import com.openroof.openroof.repository.RentalInstallmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillingService {

    private final RentalInstallmentRepository installmentRepository;
    private final RecurringChargeRepository recurringChargeRepository;
    private final LedgerEntryRepository ledgerEntryRepository;

    private static final int MONTH_TO_MONTH_DEFAULT_MONTHS = 12;

    public List<RentalInstallment> generateInstallments(Lease lease) {
        if (installmentRepository.existsByLeaseId(lease.getId())) {
            throw new IllegalStateException(
                    "Installments already generated for lease id=" + lease.getId());
        }

        List<RecurringCharge> charges = recurringChargeRepository
                .findByLeaseIdAndIsActiveTrue(lease.getId());
        List<RentalInstallment> installments = buildInstallments(lease, charges);
        List<RentalInstallment> saved;
        try {
            saved = installmentRepository.saveAll(installments);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException(
                    "Installments already generated for lease id=" + lease.getId(), e);
        }

        List<LedgerEntry> ledgerEntries = buildLedgerEntries(lease, saved);
        ledgerEntryRepository.saveAll(ledgerEntries);

        log.info("Generated {} installments and ledger entries for lease id={}",
                saved.size(), lease.getId());
        return saved;
    }

    public List<RentalInstallment> extendInstallments(Lease lease, int additionalMonths) {
        if (lease.getLeaseType() != LeaseType.MONTH_TO_MONTH) {
            throw new IllegalArgumentException(
                    "extendInstallments only supported for MONTH_TO_MONTH leases");
        }

        List<RentalInstallment> existing = installmentRepository
                .findByLeaseIdOrderByDueDateAsc(lease.getId());

        if (existing.isEmpty()) {
            return generateInstallments(lease);
        }

        RentalInstallment last = existing.get(existing.size() - 1);
        LocalDate periodStart = last.getPeriodEnd().plusDays(1);
        int nextNumber = last.getInstallmentNumber() + 1;

        List<RentalInstallment> newInstallments = new ArrayList<>();
        List<RecurringCharge> charges = recurringChargeRepository
                .findByLeaseIdAndIsActiveTrue(lease.getId());
        for (int i = 0; i < additionalMonths; i++) {
            YearMonth ym = YearMonth.from(periodStart);
            LocalDate periodEnd = ym.atEndOfMonth();

            RentalInstallment inst = buildSingleInstallment(
                    lease, nextNumber + i, periodStart, periodEnd, charges);
            newInstallments.add(inst);

            periodStart = ym.plusMonths(1).atDay(1);
        }

        List<RentalInstallment> saved = installmentRepository.saveAll(newInstallments);

        List<LedgerEntry> ledgerEntries = buildLedgerEntries(lease, saved);
        ledgerEntryRepository.saveAll(ledgerEntries);

        log.info("Extended lease id={} by {} months ({} new installments)",
                lease.getId(), additionalMonths, saved.size());
        return saved;
    }

    private List<RentalInstallment> buildInstallments(Lease lease,
                                                       List<RecurringCharge> charges) {
        int totalPeriods = calculateTotalPeriods(lease);
        List<RentalInstallment> installments = new ArrayList<>(totalPeriods);

        LocalDate periodStart = resolveEffectiveStartDate(lease);
        boolean proratedFirstMonth = lease.getMoveInDate() != null
                && lease.getMoveInDate().getDayOfMonth() != 1;

        BillingFrequency frequency = lease.getBillingFrequency() != null
                ? lease.getBillingFrequency()
                : BillingFrequency.MONTHLY;

        for (int i = 1; i <= totalPeriods; i++) {
            LocalDate periodEnd = calculatePeriodEnd(periodStart, lease.getLeaseType(),
                    lease.getEndDate(), frequency, i == totalPeriods);

            RentalInstallment inst = buildSingleInstallment(
                    lease, i, periodStart, periodEnd, charges);

            if (i == 1 && proratedFirstMonth) {
                BigDecimal proratedRent = calculateProratedRent(
                        lease.getMonthlyRent(), lease.getMoveInDate());
                inst.setBaseRent(proratedRent);

                BigDecimal otherChargesSum = sumActiveRecurringChargeAmounts(
                        charges, periodStart, periodEnd);
                inst.setTotalAmount(proratedRent.add(otherChargesSum));
            } else if (i == totalPeriods
                    && lease.getLeaseType() == LeaseType.FIXED_TERM
                    && isLastMonthPartial(lease.getEndDate())) {
                BigDecimal lastMonthRent = calculateLastMonthProratedRent(
                        lease.getMonthlyRent(), periodStart, lease.getEndDate());
                inst.setBaseRent(lastMonthRent);

                BigDecimal otherChargesSum = sumActiveRecurringChargeAmounts(
                        charges, periodStart, periodEnd);
                inst.setTotalAmount(lastMonthRent.add(otherChargesSum));
            }

            installments.add(inst);
            periodStart = periodEnd.plusDays(1);
        }

        return installments;
    }

    private RentalInstallment buildSingleInstallment(Lease lease, int number,
                                                     LocalDate periodStart,
                                                     LocalDate periodEnd,
                                                     List<RecurringCharge> charges) {
        BigDecimal otherChargesAmount = sumActiveRecurringChargeAmounts(
                charges, periodStart, periodEnd);
        Map<String, Object> otherChargesMap = buildOtherChargesMap(
                charges, periodStart, periodEnd);
        BigDecimal totalAmount = lease.getMonthlyRent().add(otherChargesAmount);

        return RentalInstallment.builder()
                .lease(lease)
                .installmentNumber(number)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .dueDate(calculateDueDate(periodStart, lease.getDueDay()))
                .baseRent(lease.getMonthlyRent())
                .otherCharges(otherChargesMap.isEmpty() ? null : otherChargesMap)
                .totalAmount(totalAmount)
                .paidAmount(BigDecimal.ZERO)
                .lateFee(BigDecimal.ZERO)
                .status(InstallmentStatus.PENDING)
                .invoiceNumber(buildInvoiceNumber(lease.getId(), number))
                .build();
    }

    private List<LedgerEntry> buildLedgerEntries(Lease lease,
                                                  List<RentalInstallment> installments) {
        List<LedgerEntry> entries = new ArrayList<>(installments.size());
        String propertyTitle = resolvePropertyTitle(lease);
        for (RentalInstallment inst : installments) {
            entries.add(LedgerEntry.builder()
                    .lease(lease)
                    .property(lease.getProperty())
                    .date(inst.getDueDate())
                    .type(LedgerEntryType.CREDIT)
                    .category(LedgerEntryCategory.RENT)
                    .amount(inst.getTotalAmount())
                    .currency(lease.getCurrency() != null ? lease.getCurrency() : "USD")
                    .description("Cuota #" + inst.getInstallmentNumber()
                            + " \u2014 " + propertyTitle)
                    .build());
        }
        return entries;
    }

    private String resolvePropertyTitle(Lease lease) {
        if (lease.getProperty() == null) {
            return "Property unknown";
        }
        String title = lease.getProperty().getTitle();
        if (title != null && !title.isBlank()) {
            return title;
        }
        Long propertyId = lease.getProperty().getId();
        if (propertyId != null) {
            return "Property #" + propertyId;
        }
        return "Property unknown";
    }

    int calculateTotalPeriods(Lease lease) {
        if (lease.getLeaseType() == LeaseType.MONTH_TO_MONTH) {
            return MONTH_TO_MONTH_DEFAULT_MONTHS;
        }
        BillingFrequency frequency = lease.getBillingFrequency() != null
                ? lease.getBillingFrequency()
                : BillingFrequency.MONTHLY;

        int monthsBetween = monthsBetween(lease.getStartDate(), lease.getEndDate());
        int monthsPerPeriod = monthsPerFrequency(frequency);

        return (int) Math.ceil((double) monthsBetween / monthsPerPeriod);
    }

    LocalDate resolveEffectiveStartDate(Lease lease) {
        if (lease.getMoveInDate() != null) {
            return lease.getMoveInDate();
        }
        return lease.getStartDate();
    }

    LocalDate calculatePeriodEnd(LocalDate periodStart, LeaseType leaseType,
                                  LocalDate leaseEndDate, BillingFrequency frequency,
                                  boolean isLastPeriod) {
        if (leaseType == LeaseType.FIXED_TERM && isLastPeriod) {
            return leaseEndDate;
        }

        YearMonth ym = YearMonth.from(periodStart);
        int monthsToAdd = monthsPerFrequency(frequency);
        YearMonth nextYm = ym.plusMonths(monthsToAdd);

        if (leaseType == LeaseType.FIXED_TERM
                && !nextYm.minusMonths(1).atEndOfMonth().isBefore(leaseEndDate)) {
            return leaseEndDate;
        }

        return nextYm.minusMonths(1).atEndOfMonth();
    }

    LocalDate calculateDueDate(LocalDate periodStart, Integer dueDay) {
        int day = dueDay != null ? dueDay : 1;
        YearMonth ym = YearMonth.from(periodStart);
        int lastDay = ym.lengthOfMonth();
        int clampedDay = Math.min(day, lastDay);

        LocalDate candidate = ym.atDay(clampedDay);
        if (candidate.isBefore(periodStart)) {
            return periodStart;
        }
        return candidate;
    }

    BigDecimal calculateProratedRent(BigDecimal monthlyRent, LocalDate moveInDate) {
        YearMonth ym = YearMonth.from(moveInDate);
        int daysInMonth = ym.lengthOfMonth();
        int daysRemaining = daysInMonth - moveInDate.getDayOfMonth() + 1;

        BigDecimal dailyRate = monthlyRent.divide(
                BigDecimal.valueOf(daysInMonth), 10, RoundingMode.HALF_UP);
        return dailyRate.multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, RoundingMode.HALF_UP);
    }

    boolean isLastMonthPartial(LocalDate endDate) {
        return endDate.getDayOfMonth() != endDate.lengthOfMonth();
    }

    BigDecimal calculateLastMonthProratedRent(BigDecimal monthlyRent,
                                               LocalDate periodStart,
                                               LocalDate endDate) {
        YearMonth finalMonth = YearMonth.from(endDate);
        int daysInMonth = finalMonth.lengthOfMonth();
        int daysRemaining = endDate.getDayOfMonth();

        BigDecimal partialRent = monthlyRent
                .divide(BigDecimal.valueOf(daysInMonth), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(daysRemaining))
                .setScale(2, RoundingMode.HALF_UP);

        long fullMonthsInPeriod = java.time.temporal.ChronoUnit.MONTHS.between(
                YearMonth.from(periodStart), finalMonth);
        if (fullMonthsInPeriod > 0) {
            BigDecimal fullMonthsRent = monthlyRent.multiply(
                    BigDecimal.valueOf(fullMonthsInPeriod));
            return fullMonthsRent.add(partialRent);
        }

        return partialRent;
    }

    private BigDecimal sumActiveRecurringChargeAmounts(List<RecurringCharge> charges,
                                                        LocalDate periodStart,
                                                        LocalDate periodEnd) {
        BigDecimal total = BigDecimal.ZERO;
        for (RecurringCharge charge : charges) {
            if (isChargeActiveInPeriod(charge, periodStart, periodEnd)) {
                total = total.add(charge.getAmount());
            }
        }
        return total;
    }

    private Map<String, Object> buildOtherChargesMap(List<RecurringCharge> charges,
                                                      LocalDate periodStart,
                                                      LocalDate periodEnd) {
        Map<String, Object> map = new HashMap<>();
        for (RecurringCharge charge : charges) {
            if (isChargeActiveInPeriod(charge, periodStart, periodEnd)) {
                map.put(charge.getDescription(), charge.getAmount());
            }
        }
        return map;
    }

    private boolean isChargeActiveInPeriod(RecurringCharge charge,
                                           LocalDate periodStart,
                                           LocalDate periodEnd) {
        if (!Boolean.TRUE.equals(charge.getIsActive())) {
            return false;
        }
        if (charge.getStartDate() != null && charge.getStartDate().isAfter(periodEnd)) {
            return false;
        }
        if (charge.getEndDate() != null && charge.getEndDate().isBefore(periodStart)) {
            return false;
        }
        return true;
    }

    String buildInvoiceNumber(Long leaseId, int installmentNumber) {
        return String.format("INV-%d-%03d", leaseId, installmentNumber);
    }

    private int monthsBetween(LocalDate start, LocalDate end) {
        YearMonth startYm = YearMonth.from(start);
        YearMonth endYm = YearMonth.from(end);
        return (endYm.getYear() - startYm.getYear()) * 12
                + (endYm.getMonthValue() - startYm.getMonthValue()) + 1;
    }

    private int monthsPerFrequency(BillingFrequency frequency) {
        return switch (frequency) {
            case WEEKLY -> throw new IllegalArgumentException(
                    "BillingFrequency.WEEKLY is not supported for installment generation");
            case MONTHLY -> 1;
            case BIMONTHLY -> 2;
            case QUARTERLY -> 3;
            case SEMIANNUAL -> 6;
            case ANNUAL -> 12;
        };
    }
}
