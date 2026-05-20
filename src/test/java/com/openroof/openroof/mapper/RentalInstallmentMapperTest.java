package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.rental.RentalInstallmentResponse;
import com.openroof.openroof.model.enums.InstallmentStatus;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalInstallment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@code RentalInstallmentMapper — toResponse / toResponseList}.
 */
@DisplayName("RentalInstallmentMapper — toResponse / toResponseList")
class RentalInstallmentMapperTest {

    private final RentalInstallmentMapper mapper = new RentalInstallmentMapper();

    // ─── helpers ─────────────────────────────────────────────────────────────

    private RentalInstallment buildInstallment(BigDecimal base, BigDecimal late, Long leaseId) {
        Lease lease = null;
        if (leaseId != null) {
            lease = mock(Lease.class);
            when(lease.getId()).thenReturn(leaseId);
        }
        RentalInstallment inst = RentalInstallment.builder()
                .installmentNumber(1)
                .baseRent(base)
                .lateFee(late)
                .dueDate(LocalDate.of(2026, 6, 1))
                .status(InstallmentStatus.PENDING)
                .lease(lease)
                .build();
        inst.setId(10L);
        return inst;
    }

    // ─── toResponse ──────────────────────────────────────────────────────────

    /**
     * totalAmount = baseRent + lateFee.
     */
    @Test
    @DisplayName("totalAmount = baseRent + lateFee")
    void toResponse_totalAmount_isSumOfBaseAndLateFee() {
        RentalInstallment inst = buildInstallment(new BigDecimal("1000.00"), new BigDecimal("50.00"), 5L);

        RentalInstallmentResponse resp = mapper.toResponse(inst);

        assertThat(resp.totalAmount()).isEqualByComparingTo("1050.00");
        assertThat(resp.amount()).isEqualByComparingTo("1000.00");
        assertThat(resp.lateFeeAmount()).isEqualByComparingTo("50.00");
    }

    /**
     * null baseRent and null lateFee both default to ZERO; total is ZERO.
     */
    @Test
    @DisplayName("null baseRent and null lateFee both default to ZERO; total is ZERO")
    void toResponse_nullAmounts_defaultToZero() {
        RentalInstallment inst = buildInstallment(null, null, 5L);

        RentalInstallmentResponse resp = mapper.toResponse(inst);

        assertThat(resp.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.lateFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * leaseId is null when installment has no lease.
     */
    @Test
    @DisplayName("leaseId is null when installment has no lease")
    void toResponse_noLease_leaseIdIsNull() {
        RentalInstallment inst = buildInstallment(new BigDecimal("800.00"), BigDecimal.ZERO, null);

        RentalInstallmentResponse resp = mapper.toResponse(inst);

        assertThat(resp.leaseId()).isNull();
    }

    /**
     * leaseId is populated when installment has a lease.
     */
    @Test
    @DisplayName("leaseId is populated when installment has a lease")
    void toResponse_withLease_leaseIdMapped() {
        RentalInstallment inst = buildInstallment(new BigDecimal("800.00"), BigDecimal.ZERO, 42L);

        RentalInstallmentResponse resp = mapper.toResponse(inst);

        assertThat(resp.leaseId()).isEqualTo(42L);
    }

    /**
     * scalar fields are mapped verbatim.
     */
    @Test
    @DisplayName("scalar fields are mapped verbatim")
    void toResponse_scalarFieldsMapped() {
        RentalInstallment inst = buildInstallment(new BigDecimal("900.00"), new BigDecimal("20.00"), 3L);

        RentalInstallmentResponse resp = mapper.toResponse(inst);

        assertThat(resp.id()).isEqualTo(10L);
        assertThat(resp.installmentNumber()).isEqualTo(1);
        assertThat(resp.dueDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(resp.status()).isEqualTo(InstallmentStatus.PENDING);
    }

    // ─── toResponseList ──────────────────────────────────────────────────────

    /**
     * toResponseList maps all elements preserving order.
     */
    @Test
    @DisplayName("toResponseList maps all elements preserving order")
    void toResponseList_mapsAllElements() {
        List<RentalInstallment> installments = List.of(
                buildInstallment(new BigDecimal("500"), BigDecimal.ZERO, 1L),
                buildInstallment(new BigDecimal("600"), new BigDecimal("10"), 1L)
        );

        List<RentalInstallmentResponse> result = mapper.toResponseList(installments);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).amount()).isEqualByComparingTo("500");
        assertThat(result.get(1).totalAmount()).isEqualByComparingTo("610");
    }

    /**
     * toResponseList on empty list returns empty list.
     */
    @Test
    @DisplayName("toResponseList on empty list returns empty list")
    void toResponseList_empty_returnsEmpty() {
        assertThat(mapper.toResponseList(List.of())).isEmpty();
    }
}
