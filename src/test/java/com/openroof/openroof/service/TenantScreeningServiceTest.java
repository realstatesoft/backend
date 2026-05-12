package com.openroof.openroof.service;

import com.openroof.openroof.mapper.TenantScreeningMapper;
import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.screening.TenantScreening;
import com.openroof.openroof.repository.RentalApplicationRepository;
import com.openroof.openroof.repository.TenantScreeningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class TenantScreeningServiceTest {

    @Mock
    private TenantScreeningRepository screeningRepository;
    @Mock
    private RentalApplicationRepository rentalApplicationRepository;
    @Mock
    private TenantScreeningMapper mapper;

    private TenantScreeningService service;

    @BeforeEach
    void setUp() {
        service = new TenantScreeningService(screeningRepository, rentalApplicationRepository, mapper);
    }

    // ─── REJECT: evictions presentes ───────────────────────────────────────────

    @Test
    void applyRules_withEvictions_returnsReject() {
        TenantScreening s = screening(buildApplication(new BigDecimal("5"), null, null),
                BackgroundCheckStatus.CLEAR);
        s.setEvictionHistory(List.of(Map.of("year", 2022)));

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.REJECT, s.getRecommendation());
    }

    @Test
    void applyRules_withEvictions_overridesGoodRatioAndClear() {
        // Aunque ratio>=3 y background CLEAR, evictions fuerzan REJECT
        TenantScreening s = screening(buildApplication(new BigDecimal("4"), null, null),
                BackgroundCheckStatus.CLEAR);
        s.setEvictionHistory(List.of(Map.of("court", "X")));

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.REJECT, s.getRecommendation());
    }

    // ─── APPROVE: ratio>=3 AND CLEAR ───────────────────────────────────────────

    @Test
    void applyRules_ratioExactly3AndClear_returnsApprove() {
        TenantScreening s = screening(buildApplication(new BigDecimal("3"), null, null),
                BackgroundCheckStatus.CLEAR);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.APPROVE, s.getRecommendation());
    }

    @Test
    void applyRules_ratioAbove3AndClear_returnsApprove() {
        TenantScreening s = screening(buildApplication(new BigDecimal("5.5"), null, null),
                BackgroundCheckStatus.CLEAR);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.APPROVE, s.getRecommendation());
    }

    // ─── REVIEW: cualquier otra cosa ───────────────────────────────────────────

    @Test
    void applyRules_ratioBelow3AndClear_returnsReview() {
        TenantScreening s = screening(buildApplication(new BigDecimal("2.99"), null, null),
                BackgroundCheckStatus.CLEAR);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.REVIEW, s.getRecommendation());
    }

    @Test
    void applyRules_ratioOkButFlagged_returnsReview() {
        TenantScreening s = screening(buildApplication(new BigDecimal("4"), null, null),
                BackgroundCheckStatus.FLAGGED);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.REVIEW, s.getRecommendation());
    }

    @Test
    void applyRules_ratioOkButFailed_returnsReview() {
        TenantScreening s = screening(buildApplication(new BigDecimal("4"), null, null),
                BackgroundCheckStatus.FAILED);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.REVIEW, s.getRecommendation());
    }

    @Test
    void applyRules_ratioOkButBackgroundNull_returnsReview() {
        TenantScreening s = screening(buildApplication(new BigDecimal("4"), null, null), null);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.REVIEW, s.getRecommendation());
    }

    @Test
    void applyRules_nullRatioAndNoIncome_returnsReview() {
        // No precomputado, no monthlyIncome ni rent: incompleto ⇒ REVIEW
        TenantScreening s = screening(buildApplication(null, null, null),
                BackgroundCheckStatus.CLEAR);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.REVIEW, s.getRecommendation());
    }

    // ─── Fallback: ratio computado desde income/rent ──────────────────────────

    @Test
    void applyRules_nullRatio_computesFromIncomeAndRent_approve() {
        // income=3000, rent=1000 ⇒ ratio=3 ⇒ APPROVE
        TenantScreening s = screening(
                buildApplication(null, new BigDecimal("3000"), new BigDecimal("1000")),
                BackgroundCheckStatus.CLEAR);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.APPROVE, s.getRecommendation());
    }

    @Test
    void applyRules_nullRatio_computesFromIncomeAndRent_review() {
        // income=1500, rent=1000 ⇒ ratio=1.5 ⇒ REVIEW
        TenantScreening s = screening(
                buildApplication(null, new BigDecimal("1500"), new BigDecimal("1000")),
                BackgroundCheckStatus.CLEAR);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.REVIEW, s.getRecommendation());
    }

    @Test
    void applyRules_rentZero_falsifiesRatio_returnsReview() {
        TenantScreening s = screening(
                buildApplication(null, new BigDecimal("3000"), BigDecimal.ZERO),
                BackgroundCheckStatus.CLEAR);

        service.applyRules(s);

        assertEquals(ScreeningRecommendation.REVIEW, s.getRecommendation());
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private TenantScreening screening(RentalApplication application, BackgroundCheckStatus bg) {
        return TenantScreening.builder()
                .application(application)
                .backgroundCheckStatus(bg)
                .build();
    }

    private RentalApplication buildApplication(BigDecimal ratio, BigDecimal monthlyIncome, BigDecimal rentAmount) {
        Property property = null;
        if (rentAmount != null) {
            property = Property.builder().rentAmount(rentAmount).build();
        }
        return RentalApplication.builder()
                .incomeToRentRatio(ratio)
                .monthlyIncome(monthlyIncome)
                .property(property)
                .build();
    }
}
