package com.openroof.openroof.screening.adapter;

import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;

import java.util.List;
import java.util.Map;

public record ScreeningResult(
        ScreeningProvider provider,
        Integer creditScore,
        BackgroundCheckStatus backgroundCheckStatus,
        List<Map<String, Object>> evictionHistory,
        List<Map<String, Object>> criminalRecords,
        Boolean incomeVerified,
        Boolean identityVerified,
        ScreeningRecommendation recommendation,
        Map<String, Object> raw
) {

    public static ScreeningResult placeholder(ScreeningProvider provider) {
        return new ScreeningResult(
                provider,
                null,
                null,
                null,
                null,
                false,
                false,
                ScreeningRecommendation.REVIEW,
                null);
    }
}
