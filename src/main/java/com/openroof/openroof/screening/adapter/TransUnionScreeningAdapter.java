package com.openroof.openroof.screening.adapter;

import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.model.rental.RentalApplication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TransUnionScreeningAdapter implements ScreeningProviderAdapter {

    @Override
    public ScreeningProvider provider() {
        return ScreeningProvider.TRANSUNION;
    }

    @Override
    public ScreeningResult runScreening(RentalApplication application) {
        long seed = (application != null && application.getId() != null) ? application.getId() : 0L;

        int creditScore = 550 + (int) (Math.floorMod(seed * 13L, 251L));
        int bucket = (int) Math.floorMod(seed, 10L);

        BackgroundCheckStatus bg;
        ScreeningRecommendation rec;
        if (bucket < 1) {
            bg = BackgroundCheckStatus.FAILED;
            rec = ScreeningRecommendation.REJECT;
        } else if (bucket < 3) {
            bg = BackgroundCheckStatus.FLAGGED;
            rec = ScreeningRecommendation.REVIEW;
        } else {
            bg = BackgroundCheckStatus.CLEAR;
            rec = ScreeningRecommendation.APPROVE;
        }

        return new ScreeningResult(
                ScreeningProvider.TRANSUNION,
                creditScore,
                bg,
                null,
                null,
                true,
                true,
                rec,
                Map.of("mock", true, "seed", seed));
    }
}
