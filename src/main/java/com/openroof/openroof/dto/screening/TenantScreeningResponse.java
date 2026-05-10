package com.openroof.openroof.dto.screening;

import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record TenantScreeningResponse(
        Long id,
        Long applicationId,
        ScreeningProvider provider,
        Integer creditScore,
        String creditReportUrl,
        BackgroundCheckStatus backgroundCheckStatus,
        String backgroundReportUrl,
        List<Map<String, Object>> evictionHistory,
        List<Map<String, Object>> criminalRecords,
        Boolean incomeVerified,
        Boolean identityVerified,
        ScreeningRecommendation recommendation,
        String notes,
        LocalDateTime expiresAt,
        LocalDateTime runAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
        
) {}
