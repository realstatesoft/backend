package com.openroof.openroof.screening.adapter;

import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.model.rental.RentalApplication;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransUnionScreeningAdapterTest {

    private final TransUnionScreeningAdapter adapter = new TransUnionScreeningAdapter();

    @Test
    void provider_returnsTransUnion() {
        assertThat(adapter.provider()).isEqualTo(ScreeningProvider.TRANSUNION);
    }

    @Test
    void runScreening_isDeterministicForSameApplicationId() {
        ScreeningResult a = adapter.runScreening(app(42L));
        ScreeningResult b = adapter.runScreening(app(42L));

        assertThat(a.recommendation()).isEqualTo(b.recommendation());
        assertThat(a.creditScore()).isEqualTo(b.creditScore());
        assertThat(a.backgroundCheckStatus()).isEqualTo(b.backgroundCheckStatus());
    }

    @Test
    void runScreening_differentApplicationIdsCanProduceDifferentResults() {
        ScreeningResult r1 = adapter.runScreening(app(0L));   // bucket 0 → FAILED
        ScreeningResult r9 = adapter.runScreening(app(9L));   // bucket 9 → CLEAR

        assertThat(r1.recommendation()).isEqualTo(ScreeningRecommendation.REJECT);
        assertThat(r9.recommendation()).isEqualTo(ScreeningRecommendation.APPROVE);
        assertThat(r1.backgroundCheckStatus()).isEqualTo(BackgroundCheckStatus.FAILED);
        assertThat(r9.backgroundCheckStatus()).isEqualTo(BackgroundCheckStatus.CLEAR);
    }

    @Test
    void runScreening_creditScoreInRange() {
        for (long id = 0; id < 50; id++) {
            ScreeningResult r = adapter.runScreening(app(id));
            assertThat(r.creditScore()).isBetween(550, 800);
        }
    }

    @Test
    void runScreening_setsVerifiedFlagsTrue() {
        ScreeningResult r = adapter.runScreening(app(5L));
        assertThat(r.incomeVerified()).isTrue();
        assertThat(r.identityVerified()).isTrue();
    }

    @Test
    void runScreening_returnsRawWithMockMarker() {
        ScreeningResult r = adapter.runScreening(app(7L));
        assertThat(r.raw()).containsEntry("mock", true);
    }

    private RentalApplication app(long id) {
        RentalApplication a = RentalApplication.builder().build();
        a.setId(id);
        return a;
    }
}
