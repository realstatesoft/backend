package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.rental.CreateLeaseRequest;
import com.openroof.openroof.dto.rental.LeaseResponse;
import com.openroof.openroof.dto.rental.LeaseSummaryResponse;
import com.openroof.openroof.model.enums.BillingFrequency;
import com.openroof.openroof.model.enums.DepositStatus;
import com.openroof.openroof.model.enums.LateFeeType;
import com.openroof.openroof.model.enums.LeaseStatus;
import com.openroof.openroof.model.enums.LeaseType;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.user.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@code LeaseMapper}.
 */
@DisplayName("LeaseMapper")
class LeaseMapperTest {

    private final LeaseMapper mapper = new LeaseMapper();

    // ─── toResponse ─────────────────────────────────────────────────────────

    /**
     * toResponse_withSignedLease_mapsAllFields.
     */
    @Test
    @DisplayName("toResponse_withSignedLease_mapsAllFields")
    void toResponse_withSignedLease_mapsAllFields() {
        Property property = mock(Property.class);
        when(property.getId()).thenReturn(1L);
        when(property.getTitle()).thenReturn("Casa Recoleta");

        User landlord = mock(User.class);
        when(landlord.getId()).thenReturn(2L);

        User tenant = mock(User.class);
        when(tenant.getId()).thenReturn(3L);
        when(tenant.getName()).thenReturn("Carlos Lopez");

        LocalDateTime landlordSignedAt = LocalDateTime.of(2025, 4, 30, 9, 0);
        LocalDateTime tenantSignedAt   = LocalDateTime.of(2025, 5, 1, 10, 0);
        LocalDateTime activatedAt      = LocalDateTime.of(2025, 5, 2, 9, 0);

        Lease lease = Lease.builder()
                .property(property)
                .landlord(landlord)
                .primaryTenant(tenant)
                .leaseType(LeaseType.FIXED_TERM)
                .status(LeaseStatus.ACTIVE)
                .startDate(LocalDate.of(2025, 5, 1))
                .endDate(LocalDate.of(2026, 5, 1))
                .monthlyRent(new BigDecimal("2000000"))
                .securityDeposit(new BigDecimal("4000000"))
                .depositStatus(DepositStatus.HELD)
                .billingFrequency(BillingFrequency.MONTHLY)
                .signedByLandlordAt(landlordSignedAt)
                .signedByTenantAt(tenantSignedAt)
                .activatedAt(activatedAt)
                .build();

        LeaseResponse dto = mapper.toResponse(lease);

        assertThat(dto.propertyId()).isEqualTo(1L);
        assertThat(dto.propertyAddress()).isEqualTo("Casa Recoleta");
        assertThat(dto.landlordId()).isEqualTo(2L);
        assertThat(dto.tenantId()).isEqualTo(3L);
        assertThat(dto.tenantName()).isEqualTo("Carlos Lopez");
        assertThat(dto.leaseType()).isEqualTo(LeaseType.FIXED_TERM);
        assertThat(dto.status()).isEqualTo(LeaseStatus.ACTIVE);
        assertThat(dto.monthlyRent()).isEqualByComparingTo("2000000");
        assertThat(dto.securityDeposit()).isEqualByComparingTo("4000000");
        assertThat(dto.depositStatus()).isEqualTo(DepositStatus.HELD);
        assertThat(dto.billingFrequency()).isEqualTo(BillingFrequency.MONTHLY);
        assertThat(dto.signedAt()).isEqualTo(tenantSignedAt);
        assertThat(dto.activatedAt()).isEqualTo(activatedAt);
    }

    /**
     * toResponse_notYetSigned_signedAtIsNull.
     */
    @Test
    @DisplayName("toResponse_notYetSigned_signedAtIsNull")
    void toResponse_notYetSigned_signedAtIsNull() {
        // isSigned() = signedByLandlordAt != null && signedByTenantAt != null → both null = false
        Lease lease = Lease.builder()
                .property(mock(Property.class))
                .landlord(mock(User.class))
                .primaryTenant(mock(User.class))
                .leaseType(LeaseType.FIXED_TERM)
                .status(LeaseStatus.DRAFT)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(6))
                .monthlyRent(BigDecimal.TEN)
                .billingFrequency(BillingFrequency.MONTHLY)
                .build();

        LeaseResponse dto = mapper.toResponse(lease);

        assertThat(dto.signedAt()).isNull();
    }

    /**
     * toResponse_onlyLandlordSigned_signedAtIsNull.
     */
    @Test
    @DisplayName("toResponse_onlyLandlordSigned_signedAtIsNull")
    void toResponse_onlyLandlordSigned_signedAtIsNull() {
        Lease lease = Lease.builder()
                .property(mock(Property.class))
                .landlord(mock(User.class))
                .primaryTenant(mock(User.class))
                .leaseType(LeaseType.FIXED_TERM)
                .status(LeaseStatus.DRAFT)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(6))
                .monthlyRent(BigDecimal.TEN)
                .billingFrequency(BillingFrequency.MONTHLY)
                .signedByLandlordAt(LocalDateTime.now())
                // signedByTenantAt = null → isSigned() = false
                .build();

        LeaseResponse dto = mapper.toResponse(lease);

        assertThat(dto.signedAt()).isNull();
    }

    /**
     * toResponse_nullProperty_returnsNullPropertyFields.
     */
    @Test
    @DisplayName("toResponse_nullProperty_returnsNullPropertyFields")
    void toResponse_nullProperty_returnsNullPropertyFields() {
        Lease lease = Lease.builder()
                .property(null)
                .landlord(mock(User.class))
                .primaryTenant(mock(User.class))
                .leaseType(LeaseType.MONTH_TO_MONTH)
                .status(LeaseStatus.DRAFT)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .monthlyRent(BigDecimal.ONE)
                .billingFrequency(BillingFrequency.MONTHLY)
                .build();

        LeaseResponse dto = mapper.toResponse(lease);

        assertThat(dto.propertyId()).isNull();
        assertThat(dto.propertyAddress()).isNull();
    }

    /**
     * toResponse_nullPrimaryTenant_returnsNullTenantFields.
     */
    @Test
    @DisplayName("toResponse_nullPrimaryTenant_returnsNullTenantFields")
    void toResponse_nullPrimaryTenant_returnsNullTenantFields() {
        Lease lease = Lease.builder()
                .property(mock(Property.class))
                .landlord(mock(User.class))
                .primaryTenant(null)
                .leaseType(LeaseType.FIXED_TERM)
                .status(LeaseStatus.DRAFT)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .monthlyRent(BigDecimal.ONE)
                .billingFrequency(BillingFrequency.MONTHLY)
                .build();

        LeaseResponse dto = mapper.toResponse(lease);

        assertThat(dto.tenantId()).isNull();
        assertThat(dto.tenantName()).isNull();
    }

    // ─── toSummaryResponse ──────────────────────────────────────────────────

    /**
     * toSummaryResponse_withFullLease_mapsAllFields.
     */
    @Test
    @DisplayName("toSummaryResponse_withFullLease_mapsAllFields")
    void toSummaryResponse_withFullLease_mapsAllFields() {
        Property property = mock(Property.class);
        when(property.getTitle()).thenReturn("Duplex Sur");

        User tenant = mock(User.class);
        when(tenant.getName()).thenReturn("Ana Garcia");

        Lease lease = Lease.builder()
                .property(property)
                .landlord(mock(User.class))
                .primaryTenant(tenant)
                .status(LeaseStatus.ACTIVE)
                .monthlyRent(new BigDecimal("1500000"))
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2026, 1, 1))
                .leaseType(LeaseType.FIXED_TERM)
                .billingFrequency(BillingFrequency.MONTHLY)
                .build();

        LeaseSummaryResponse dto = mapper.toSummaryResponse(lease);

        assertThat(dto.propertyAddress()).isEqualTo("Duplex Sur");
        assertThat(dto.tenantName()).isEqualTo("Ana Garcia");
        assertThat(dto.status()).isEqualTo(LeaseStatus.ACTIVE);
        assertThat(dto.monthlyRent()).isEqualByComparingTo("1500000");
        assertThat(dto.startDate()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(dto.endDate()).isEqualTo(LocalDate.of(2026, 1, 1));
    }

    /**
     * toSummaryResponse_nullPropertyAndTenant_returnsNulls.
     */
    @Test
    @DisplayName("toSummaryResponse_nullPropertyAndTenant_returnsNulls")
    void toSummaryResponse_nullPropertyAndTenant_returnsNulls() {
        Lease lease = Lease.builder()
                .property(null)
                .landlord(mock(User.class))
                .primaryTenant(null)
                .status(LeaseStatus.DRAFT)
                .monthlyRent(BigDecimal.TEN)
                .leaseType(LeaseType.FIXED_TERM)
                .billingFrequency(BillingFrequency.MONTHLY)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .build();

        LeaseSummaryResponse dto = mapper.toSummaryResponse(lease);

        assertThat(dto.propertyAddress()).isNull();
        assertThat(dto.tenantName()).isNull();
    }

    // ─── toEntity ───────────────────────────────────────────────────────────

    /**
     * toEntity_withValidArgs_setsStatusDraftAndAllFields.
     */
    @Test
    @DisplayName("toEntity_withValidArgs_setsStatusDraftAndAllFields")
    void toEntity_withValidArgs_setsStatusDraftAndAllFields() {
        CreateLeaseRequest dto = new CreateLeaseRequest(
                1L, 2L, LeaseType.FIXED_TERM,
                LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1),
                new BigDecimal("1500000"), new BigDecimal("3000000"),
                BillingFrequency.MONTHLY, LateFeeType.FIXED_AMOUNT,
                new BigDecimal("50000"), null);

        Lease lease = mapper.toEntity(dto, mock(Property.class), mock(User.class), mock(User.class));

        assertThat(lease.getStatus()).isEqualTo(LeaseStatus.DRAFT);
        assertThat(lease.getLeaseType()).isEqualTo(LeaseType.FIXED_TERM);
        assertThat(lease.getMonthlyRent()).isEqualByComparingTo("1500000");
        assertThat(lease.getSecurityDeposit()).isEqualByComparingTo("3000000");
        assertThat(lease.getBillingFrequency()).isEqualTo(BillingFrequency.MONTHLY);
        assertThat(lease.getLateFeeType()).isEqualTo(LateFeeType.FIXED_AMOUNT);
        assertThat(lease.getLateFeeValue()).isEqualByComparingTo("50000");
    }

    /**
     * toEntity_nullDto_throwsNPE.
     */
    @Test
    @DisplayName("toEntity_nullDto_throwsNPE")
    void toEntity_nullDto_throwsNPE() {
        assertThatNullPointerException().isThrownBy(() ->
                mapper.toEntity(null, mock(Property.class), mock(User.class), mock(User.class)));
    }

    /**
     * toEntity_nullProperty_throwsNPE.
     */
    @Test
    @DisplayName("toEntity_nullProperty_throwsNPE")
    void toEntity_nullProperty_throwsNPE() {
        CreateLeaseRequest dto = new CreateLeaseRequest(
                1L, 2L, LeaseType.FIXED_TERM, LocalDate.now(), LocalDate.now().plusMonths(12),
                BigDecimal.TEN, BigDecimal.TEN, BillingFrequency.MONTHLY, null, null, null);

        assertThatNullPointerException().isThrownBy(() ->
                mapper.toEntity(dto, null, mock(User.class), mock(User.class)));
    }

    /**
     * toEntity_nullTenant_throwsNPE.
     */
    @Test
    @DisplayName("toEntity_nullTenant_throwsNPE")
    void toEntity_nullTenant_throwsNPE() {
        CreateLeaseRequest dto = new CreateLeaseRequest(
                1L, 2L, LeaseType.FIXED_TERM, LocalDate.now(), LocalDate.now().plusMonths(12),
                BigDecimal.TEN, BigDecimal.TEN, BillingFrequency.MONTHLY, null, null, null);

        assertThatNullPointerException().isThrownBy(() ->
                mapper.toEntity(dto, mock(Property.class), null, mock(User.class)));
    }

    // ─── updateEntity ───────────────────────────────────────────────────────

    /**
     * updateEntity_updatesAllMutableFields.
     */
    @Test
    @DisplayName("updateEntity_updatesAllMutableFields")
    void updateEntity_updatesAllMutableFields() {
        Lease lease = Lease.builder()
                .leaseType(LeaseType.FIXED_TERM)
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2025, 1, 1))
                .monthlyRent(new BigDecimal("1000000"))
                .securityDeposit(new BigDecimal("2000000"))
                .billingFrequency(BillingFrequency.MONTHLY)
                .lateFeeType(LateFeeType.FIXED_AMOUNT)
                .lateFeeValue(new BigDecimal("50000"))
                .status(LeaseStatus.DRAFT)
                .landlord(mock(User.class))
                .property(mock(Property.class))
                .primaryTenant(mock(User.class))
                .build();

        CreateLeaseRequest update = new CreateLeaseRequest(
                1L, 2L, LeaseType.MONTH_TO_MONTH,
                LocalDate.of(2025, 6, 1), LocalDate.of(2026, 6, 1),
                new BigDecimal("1800000"), new BigDecimal("3600000"),
                BillingFrequency.BIMONTHLY, LateFeeType.PERCENTAGE,
                new BigDecimal("5"), null);

        mapper.updateEntity(lease, update);

        assertThat(lease.getLeaseType()).isEqualTo(LeaseType.MONTH_TO_MONTH);
        assertThat(lease.getStartDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(lease.getEndDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(lease.getMonthlyRent()).isEqualByComparingTo("1800000");
        assertThat(lease.getSecurityDeposit()).isEqualByComparingTo("3600000");
        assertThat(lease.getBillingFrequency()).isEqualTo(BillingFrequency.BIMONTHLY);
        assertThat(lease.getLateFeeType()).isEqualTo(LateFeeType.PERCENTAGE);
        assertThat(lease.getLateFeeValue()).isEqualByComparingTo("5");
    }
}
