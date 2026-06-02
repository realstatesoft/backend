package com.openroof.openroof.dto.screening;

import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.validation.MaxDigits;
import jakarta.validation.constraints.Size;

public record UpdateScreeningRequest(
        @MaxDigits(integer = 20)
        Integer creditScore,
        BackgroundCheckStatus backgroundCheckStatus,
        Boolean incomeVerified,
        Boolean identityVerified,
        ScreeningRecommendation recommendation,
        @Size(max = 2000) String notes
) {}
