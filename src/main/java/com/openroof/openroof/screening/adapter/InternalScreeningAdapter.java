package com.openroof.openroof.screening.adapter;

import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.screening.TenantScreening;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class InternalScreeningAdapter implements ScreeningProviderAdapter {

    private static final BigDecimal MIN_INCOME_TO_RENT_RATIO = new BigDecimal("3");

    @Override
    public ScreeningProvider provider() {
        return ScreeningProvider.INTERNAL;
    }

    @Override
    public ScreeningResult runScreening(RentalApplication application) {
        return ScreeningResult.placeholder(ScreeningProvider.INTERNAL);
    }

    public ScreeningRecommendation applyRules(TenantScreening screening) {
        if (hasEvictions(screening)) {
            return ScreeningRecommendation.REJECT;
        }
        BigDecimal ratio = resolveIncomeToRentRatio(screening.getApplication());
        boolean ratioOk = ratio != null && ratio.compareTo(MIN_INCOME_TO_RENT_RATIO) >= 0;
        boolean backgroundClear = screening.getBackgroundCheckStatus() == BackgroundCheckStatus.CLEAR;
        return ratioOk && backgroundClear
                ? ScreeningRecommendation.APPROVE
                : ScreeningRecommendation.REVIEW;
    }

    private static boolean hasEvictions(TenantScreening screening) {
        return screening.getEvictionHistory() != null && !screening.getEvictionHistory().isEmpty();
    }

    private static BigDecimal resolveIncomeToRentRatio(RentalApplication application) {
        if (application == null) {
            return null;
        }
        if (application.getIncomeToRentRatio() != null) {
            return application.getIncomeToRentRatio();
        }
        BigDecimal income = application.getMonthlyIncome();
        Property property = application.getProperty();
        BigDecimal rent = property != null ? property.getRentAmount() : null;
        if (income == null || rent == null || rent.signum() <= 0) {
            return null;
        }
        return income.divide(rent, 2, RoundingMode.HALF_UP);
    }
}
