package com.openroof.openroof.screening.adapter;

import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.screening.TenantScreening;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InternalScreeningAdapterTest {

    private final InternalScreeningAdapter adapter = new InternalScreeningAdapter();

    @Test
    void provider_returnsInternal() {
        assertThat(adapter.provider()).isEqualTo(ScreeningProvider.INTERNAL);
    }

    @Test
    void runScreening_returnsReviewPlaceholder() {
        RentalApplication app = RentalApplication.builder().build();
        app.setId(1L);

        ScreeningResult res = adapter.runScreening(app);

        assertThat(res.provider()).isEqualTo(ScreeningProvider.INTERNAL);
        assertThat(res.recommendation()).isEqualTo(ScreeningRecommendation.REVIEW);
        assertThat(res.creditScore()).isNull();
        assertThat(res.backgroundCheckStatus()).isNull();
    }

    // ─── applyRules: evictions overrides everything → REJECT ──────────────────

    @Test
    void applyRules_withEvictions_returnsReject() {
        TenantScreening s = screening(buildApp(new BigDecimal("5"), null, null), BackgroundCheckStatus.CLEAR);
        s.setEvictionHistory(List.of(Map.of("year", 2022)));

        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.REJECT);
    }

    @Test
    void applyRules_evictionsOverrideGoodRatioAndClear() {
        TenantScreening s = screening(buildApp(new BigDecimal("4"), null, null), BackgroundCheckStatus.CLEAR);
        s.setEvictionHistory(List.of(Map.of("court", "X")));

        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.REJECT);
    }

    // ─── APPROVE: ratio>=3 AND CLEAR ──────────────────────────────────────────

    @Test
    void applyRules_ratioExactly3AndClear_returnsApprove() {
        TenantScreening s = screening(buildApp(new BigDecimal("3"), null, null), BackgroundCheckStatus.CLEAR);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.APPROVE);
    }

    @Test
    void applyRules_ratioAbove3AndClear_returnsApprove() {
        TenantScreening s = screening(buildApp(new BigDecimal("5.5"), null, null), BackgroundCheckStatus.CLEAR);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.APPROVE);
    }

    // ─── REVIEW: cualquier otra cosa ──────────────────────────────────────────

    @Test
    void applyRules_ratioBelow3AndClear_returnsReview() {
        TenantScreening s = screening(buildApp(new BigDecimal("2.99"), null, null), BackgroundCheckStatus.CLEAR);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.REVIEW);
    }

    @Test
    void applyRules_ratioOkButFlagged_returnsReview() {
        TenantScreening s = screening(buildApp(new BigDecimal("4"), null, null), BackgroundCheckStatus.FLAGGED);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.REVIEW);
    }

    @Test
    void applyRules_ratioOkButFailed_returnsReview() {
        TenantScreening s = screening(buildApp(new BigDecimal("4"), null, null), BackgroundCheckStatus.FAILED);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.REVIEW);
    }

    @Test
    void applyRules_ratioOkButBackgroundNull_returnsReview() {
        TenantScreening s = screening(buildApp(new BigDecimal("4"), null, null), null);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.REVIEW);
    }

    @Test
    void applyRules_nullRatioAndNoIncome_returnsReview() {
        TenantScreening s = screening(buildApp(null, null, null), BackgroundCheckStatus.CLEAR);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.REVIEW);
    }

    // ─── Fallback: ratio computado desde income/rent ──────────────────────────

    @Test
    void applyRules_nullRatio_computesFromIncomeAndRent_approve() {
        TenantScreening s = screening(buildApp(null, new BigDecimal("3000"), new BigDecimal("1000")),
                BackgroundCheckStatus.CLEAR);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.APPROVE);
    }

    @Test
    void applyRules_nullRatio_computesFromIncomeAndRent_review() {
        TenantScreening s = screening(buildApp(null, new BigDecimal("1500"), new BigDecimal("1000")),
                BackgroundCheckStatus.CLEAR);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.REVIEW);
    }

    @Test
    void applyRules_rentZero_falsifiesRatio_returnsReview() {
        TenantScreening s = screening(buildApp(null, new BigDecimal("3000"), BigDecimal.ZERO),
                BackgroundCheckStatus.CLEAR);
        assertThat(adapter.applyRules(s)).isEqualTo(ScreeningRecommendation.REVIEW);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private TenantScreening screening(RentalApplication app, BackgroundCheckStatus bg) {
        return TenantScreening.builder()
                .application(app)
                .backgroundCheckStatus(bg)
                .build();
    }

    private RentalApplication buildApp(BigDecimal ratio, BigDecimal income, BigDecimal rent) {
        Property property = rent == null ? null : Property.builder().rentAmount(rent).build();
        return RentalApplication.builder()
                .incomeToRentRatio(ratio)
                .monthlyIncome(income)
                .property(property)
                .build();
    }
}
