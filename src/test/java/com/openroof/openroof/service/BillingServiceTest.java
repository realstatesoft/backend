package com.openroof.openroof.service;

import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.enums.LedgerEntryCategory;
import com.openroof.openroof.model.enums.LedgerEntryType;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.ledger.LedgerEntry;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RecurringCharge;
import com.openroof.openroof.model.rental.RentalInstallment;
import com.openroof.openroof.repository.LedgerEntryRepository;
import com.openroof.openroof.repository.RecurringChargeRepository;
import com.openroof.openroof.repository.RentalInstallmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private RentalInstallmentRepository installmentRepository;
    @Mock
    private RecurringChargeRepository recurringChargeRepository;
    @Mock
    private LedgerEntryRepository ledgerEntryRepository;

    @InjectMocks
    private BillingService billingService;

    private Property property;
    private Lease lease;

    @BeforeEach
    void setUp() {
        property = Property.builder().title("Test Property").build();
        property.setId(100L);

        lease = Lease.builder()
                .property(property)
                .leaseType(LeaseType.FIXED_TERM)
                .status(LeaseStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 6, 1))
                .endDate(LocalDate.of(2027, 5, 31))
                .monthlyRent(new BigDecimal("150000"))
                .currency("ARS")
                .billingFrequency(BillingFrequency.MONTHLY)
                .dueDay(5)
                .gracePeriodDays(5)
                .build();
        lease.setId(42L);
    }

    private void stubDefaultInstallmentGeneration(List<RecurringCharge> charges) {
        when(installmentRepository.existsByLeaseId(42L)).thenReturn(false);
        when(recurringChargeRepository.findByLeaseIdAndIsActiveTrue(42L))
                .thenReturn(charges);
        when(installmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
        when(ledgerEntryRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void stubDefaultInstallmentGeneration() {
        stubDefaultInstallmentGeneration(List.of());
    }

    @Nested
    @DisplayName("generateInstallments")
    class GenerateInstallments {

        @Test
        @DisplayName("FIXED_TERM de 12 meses genera exactamente 12 cuotas")
        void fixedTerm12MonthsGenerates12Installments() {
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            assertThat(result).hasSize(12);
            assertThat(result.get(0).getInstallmentNumber()).isEqualTo(1);
            assertThat(result.get(11).getInstallmentNumber()).isEqualTo(12);
        }

        @Test
        @DisplayName("Todas las cuotas se crean en estado PENDING con paidAmount en ZERO")
        void allInstallmentsCreatedAsPending() {
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            for (RentalInstallment inst : result) {
                assertThat(inst.getStatus()).isEqualTo(InstallmentStatus.PENDING);
                assertThat(inst.getPaidAmount()).isEqualByComparingTo(BigDecimal.ZERO);
                assertThat(inst.getLateFee()).isEqualByComparingTo(BigDecimal.ZERO);
            }
        }

        @Test
        @DisplayName("invoiceNumber con formato INV-{leaseId}-{nnn}")
        void invoiceNumberFormat() {
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            assertThat(result.get(0).getInvoiceNumber()).isEqualTo("INV-42-001");
            assertThat(result.get(4).getInvoiceNumber()).isEqualTo("INV-42-005");
            assertThat(result.get(11).getInvoiceNumber()).isEqualTo("INV-42-012");
        }

        @Test
        @DisplayName("Debe lanzar IllegalStateException si ya existen cuotas (idempotencia)")
        void throwsExceptionWhenInstallmentsAlreadyExist() {
            when(installmentRepository.existsByLeaseId(42L)).thenReturn(true);

            assertThatThrownBy(() -> billingService.generateInstallments(lease))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("already generated");

            verify(installmentRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("Genera un LedgerEntry por cada cuota")
        void generatesLedgerEntryPerInstallment() {
            stubDefaultInstallmentGeneration();

            billingService.generateInstallments(lease);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
            verify(ledgerEntryRepository).saveAll(captor.capture());
            List<LedgerEntry> entries = captor.getValue();

            assertThat(entries).hasSize(12);
            LedgerEntry first = entries.get(0);
            assertThat(first.getType()).isEqualTo(LedgerEntryType.CREDIT);
            assertThat(first.getCategory()).isEqualTo(LedgerEntryCategory.RENT);
            assertThat(first.getCurrency()).isEqualTo("ARS");
            assertThat(first.getDescription()).contains("Cuota #1");
            assertThat(first.getDescription()).contains("Test Property");
        }

        @Test
        @DisplayName("LedgerEntry description usa fallback si property title es null")
        void ledgerEntryFallbackWhenTitleNull() {
            property.setTitle(null);
            stubDefaultInstallmentGeneration();

            billingService.generateInstallments(lease);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<LedgerEntry>> captor = ArgumentCaptor.forClass(List.class);
            verify(ledgerEntryRepository).saveAll(captor.capture());
            assertThat(captor.getValue().get(0).getDescription()).contains("Property #100");
        }
    }

    @Nested
    @DisplayName("Prorateo del primer mes")
    class ProratedFirstMonth {

        @Test
        @DisplayName("Primer mes prorrateado si moveInDate no es el dia 1")
        void proratesWhenMoveInDateNotFirstOfMonth() {
            lease.setMoveInDate(LocalDate.of(2026, 6, 15));
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            RentalInstallment first = result.get(0);
            assertThat(first.getPeriodStart()).isEqualTo(LocalDate.of(2026, 6, 15));
            assertThat(first.getPeriodEnd()).isEqualTo(LocalDate.of(2026, 6, 30));

            RentalInstallment second = result.get(1);
            assertThat(second.getPeriodStart()).isEqualTo(LocalDate.of(2026, 7, 1));
        }

        @Test
        @DisplayName("Monto prorrateado correcto: moveInDate = dia 15 de mes 30")
        void proratedAmountCorrect() {
            lease.setMoveInDate(LocalDate.of(2026, 6, 15));
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            RentalInstallment first = result.get(0);
            BigDecimal expected = new BigDecimal("80000.00");
            assertThat(first.getBaseRent()).isEqualByComparingTo(expected);

            RentalInstallment second = result.get(1);
            assertThat(second.getBaseRent()).isEqualByComparingTo(new BigDecimal("150000"));
        }

        @Test
        @DisplayName("Sin prorateo si moveInDate es null")
        void noProrateWhenMoveInDateIsNull() {
            lease.setMoveInDate(null);
            lease.setStartDate(LocalDate.of(2026, 6, 1));
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            assertThat(result.get(0).getBaseRent()).isEqualByComparingTo(new BigDecimal("150000"));
        }
    }

    @Nested
    @DisplayName("Due date")
    class DueDate {

        @Test
        @DisplayName("dueDate calculado como el dia dueDay del mes")
        void dueDateCalculatedFromDueDay() {
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            assertThat(result.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 6, 5));
        }

        @Test
        @DisplayName("dueDay=31 en febrero se clampa al ultimo dia del mes")
        void dueDayClampedToMonthEnd() {
            lease.setStartDate(LocalDate.of(2026, 2, 1));
            lease.setEndDate(LocalDate.of(2026, 2, 28));
            lease.setDueDay(31);
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            assertThat(result.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        }

        @Test
        @DisplayName("dueDay por defecto es 1 si no se especifica")
        void dueDayDefaultsTo1() {
            lease.setDueDay(null);
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            assertThat(result.get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        }
    }

    @Nested
    @DisplayName("Recurring charges")
    class RecurringCharges {

        @Test
        @DisplayName("totalAmount incluye los RecurringCharge activos del periodo")
        void includesActiveRecurringCharges() {
            RecurringCharge parking = RecurringCharge.builder()
                    .description("parking")
                    .amount(new BigDecimal("50000"))
                    .frequency(BillingFrequency.MONTHLY)
                    .startDate(LocalDate.of(2026, 1, 1))
                    .isActive(true)
                    .build();
            RecurringCharge expenses = RecurringCharge.builder()
                    .description("expenses")
                    .amount(new BigDecimal("30000"))
                    .frequency(BillingFrequency.MONTHLY)
                    .startDate(LocalDate.of(2026, 1, 1))
                    .isActive(true)
                    .build();

            stubDefaultInstallmentGeneration(List.of(parking, expenses));

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            BigDecimal expectedTotal = new BigDecimal("230000.00");
            assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo(expectedTotal);
            assertThat(result.get(0).getBaseRent()).isEqualByComparingTo(new BigDecimal("150000"));
            assertThat(result.get(0).getOtherCharges())
                    .containsEntry("parking", new BigDecimal("50000"))
                    .containsEntry("expenses", new BigDecimal("30000"));
        }

        @Test
        @DisplayName("RecurringCharge inactivo no se suma")
        void inactiveChargeNotIncluded() {
            RecurringCharge active = RecurringCharge.builder()
                    .description("parking")
                    .amount(new BigDecimal("50000"))
                    .frequency(BillingFrequency.MONTHLY)
                    .startDate(LocalDate.of(2026, 1, 1))
                    .isActive(true)
                    .build();

            stubDefaultInstallmentGeneration(List.of(active));

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            BigDecimal expectedTotal = new BigDecimal("200000.00");
            assertThat(result.get(0).getTotalAmount()).isEqualByComparingTo(expectedTotal);
        }
    }

    @Nested
    @DisplayName("MONTH_TO_MONTH")
    class MonthToMonth {

        @Test
        @DisplayName("MONTH_TO_MONTH genera 12 cuotas por defecto")
        void monthToMonthGenerates12Installments() {
            lease.setLeaseType(LeaseType.MONTH_TO_MONTH);
            lease.setEndDate(LocalDate.of(2027, 5, 31));
            stubDefaultInstallmentGeneration();

            List<RentalInstallment> result = billingService.generateInstallments(lease);

            assertThat(result).hasSize(12);
        }
    }

    @Nested
    @DisplayName("extendInstallments")
    class ExtendInstallments {

        @Test
        @DisplayName("extiende cuotas para MONTH_TO_MONTH agregando meses adicionales")
        void extendsMonthToMonth() {
            lease.setLeaseType(LeaseType.MONTH_TO_MONTH);
            RentalInstallment lastExisting = RentalInstallment.builder()
                    .installmentNumber(12)
                    .periodStart(LocalDate.of(2026, 5, 1))
                    .periodEnd(LocalDate.of(2026, 5, 31))
                    .dueDate(LocalDate.of(2026, 5, 5))
                    .build();

            when(installmentRepository.findByLeaseIdOrderByDueDateAsc(42L))
                    .thenReturn(List.of(lastExisting));
            when(recurringChargeRepository.findByLeaseIdAndIsActiveTrue(42L))
                    .thenReturn(List.of());
            when(installmentRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
            when(ledgerEntryRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            List<RentalInstallment> result = billingService.extendInstallments(lease, 3);

            assertThat(result).hasSize(3);
            assertThat(result.get(0).getInstallmentNumber()).isEqualTo(13);
            assertThat(result.get(0).getPeriodStart()).isEqualTo(LocalDate.of(2026, 6, 1));
            assertThat(result.get(2).getInstallmentNumber()).isEqualTo(15);
        }

        @Test
        @DisplayName("extendInstallments lanza excepcion para FIXED_TERM")
        void extendThrowsForFixedTerm() {
            assertThatThrownBy(() -> billingService.extendInstallments(lease, 3))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("MONTH_TO_MONTH");
        }
    }

    @Nested
    @DisplayName("buildInvoiceNumber")
    class BuildInvoiceNumber {

        @Test
        @DisplayName("genera numero de factura con padding de 3 digitos")
        void padsToThreeDigits() {
            assertThat(billingService.buildInvoiceNumber(7L, 1)).isEqualTo("INV-7-001");
            assertThat(billingService.buildInvoiceNumber(42L, 99)).isEqualTo("INV-42-099");
            assertThat(billingService.buildInvoiceNumber(100L, 100)).isEqualTo("INV-100-100");
        }
    }

    @Nested
    @DisplayName("calculateTotalPeriods")
    class CalculateTotalPeriods {

        @Test
        @DisplayName("FIXED_TERM con billing MONTHLY devuelve la cantidad de meses entre fechas")
        void fixedTermMonthlyReturnsMonthsBetween() {
            int periods = billingService.calculateTotalPeriods(lease);
            assertThat(periods).isEqualTo(12);
        }

        @Test
        @DisplayName("BillingFrequency.WEEKLY lanza IllegalArgumentException")
        void weeklyThrowsException() {
            lease.setBillingFrequency(BillingFrequency.WEEKLY);

            assertThatThrownBy(() -> billingService.calculateTotalPeriods(lease))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("WEEKLY");
        }

        @Test
        @DisplayName("FIXED_TERM con billing BIMONTHLY divide periodos correctamente")
        void bimonthlyDividesCorrectly() {
            lease.setBillingFrequency(BillingFrequency.BIMONTHLY);
            int periods = billingService.calculateTotalPeriods(lease);
            assertThat(periods).isEqualTo(6);
        }

        @Test
        @DisplayName("FIXED_TERM con billing QUARTERLY divide periodos correctamente")
        void quarterlyDividesCorrectly() {
            lease.setBillingFrequency(BillingFrequency.QUARTERLY);
            int periods = billingService.calculateTotalPeriods(lease);
            assertThat(periods).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("calculatePeriodEnd")
    class CalculatePeriodEnd {

        @Test
        @DisplayName("BIMONTHLY frequency spans 2 months")
        void bimonthlySpansTwoMonths() {
            LocalDate result = billingService.calculatePeriodEnd(
                    LocalDate.of(2026, 6, 1), LeaseType.FIXED_TERM,
                    LocalDate.of(2027, 5, 31), BillingFrequency.BIMONTHLY, false);

            assertThat(result).isEqualTo(LocalDate.of(2026, 7, 31));
        }

        @Test
        @DisplayName("QUARTERLY frequency spans 3 months")
        void quarterlySpansThreeMonths() {
            LocalDate result = billingService.calculatePeriodEnd(
                    LocalDate.of(2026, 6, 1), LeaseType.FIXED_TERM,
                    LocalDate.of(2027, 5, 31), BillingFrequency.QUARTERLY, false);

            assertThat(result).isEqualTo(LocalDate.of(2026, 8, 31));
        }

        @Test
        @DisplayName("last period returns lease end date for FIXED_TERM")
        void lastPeriodReturnsLeaseEndDate() {
            LocalDate result = billingService.calculatePeriodEnd(
                    LocalDate.of(2027, 5, 1), LeaseType.FIXED_TERM,
                    LocalDate.of(2027, 5, 31), BillingFrequency.MONTHLY, true);

            assertThat(result).isEqualTo(LocalDate.of(2027, 5, 31));
        }

        @Test
        @DisplayName("MONTH_TO_MONTH last period does not return lease end date")
        void monthToMonthLastPeriodUsesFrequency() {
            LocalDate result = billingService.calculatePeriodEnd(
                    LocalDate.of(2026, 6, 1), LeaseType.MONTH_TO_MONTH,
                    LocalDate.of(2027, 5, 31), BillingFrequency.MONTHLY, true);

            assertThat(result).isEqualTo(LocalDate.of(2026, 6, 30));
        }
    }

    @Nested
    @DisplayName("calculateDueDate")
    class CalculateDueDateUnit {

        @Test
        @DisplayName("dueDay normal dentro del mes")
        void normalDueDay() {
            LocalDate result = billingService.calculateDueDate(
                    LocalDate.of(2026, 6, 1), 10);
            assertThat(result).isEqualTo(LocalDate.of(2026, 6, 10));
        }

        @Test
        @DisplayName("dueDay mayor a dias del mes se clampa")
        void dueDayExceedsMonthDays() {
            LocalDate result = billingService.calculateDueDate(
                    LocalDate.of(2026, 2, 1), 31);
            assertThat(result).isEqualTo(LocalDate.of(2026, 2, 28));
        }

        @Test
        @DisplayName("dueDay antes de periodStart usa periodStart")
        void dueDayBeforePeriodStart() {
            LocalDate result = billingService.calculateDueDate(
                    LocalDate.of(2026, 6, 20), 5);
            assertThat(result).isEqualTo(LocalDate.of(2026, 6, 20));
        }
    }
}
