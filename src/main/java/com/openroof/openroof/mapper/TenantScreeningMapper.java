package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.screening.TenantScreeningResponse;
import com.openroof.openroof.dto.screening.UpdateScreeningRequest;
import com.openroof.openroof.model.screening.TenantScreening;
import org.springframework.stereotype.Component;

@Component
public class TenantScreeningMapper {

    public TenantScreeningResponse toResponse(TenantScreening s) {
        return new TenantScreeningResponse(
                s.getId(),
                s.getApplication() != null ? s.getApplication().getId() : null,
                s.getProvider(),
                s.getCreditScore(),
                s.getCreditReportUrl(),
                s.getBackgroundCheckStatus(),
                s.getBackgroundReportUrl(),
                s.getEvictionHistory(),
                s.getCriminalRecords(),
                s.getIncomeVerified(),
                s.getIdentityVerified(),
                s.getRecommendation(),
                s.getNotes(),
                s.getExpiresAt(),
                s.getRunAt(),
                s.getCreatedAt(),
                s.getUpdatedAt()
        );
    }

    public void updateEntity(UpdateScreeningRequest req, TenantScreening s) {
        if (req.creditScore() != null) {
            s.setCreditScore(req.creditScore());
        }
        if (req.backgroundCheckStatus() != null) {
            s.setBackgroundCheckStatus(req.backgroundCheckStatus());
        }
        if (req.incomeVerified() != null) {
            s.setIncomeVerified(req.incomeVerified());
        }
        if (req.identityVerified() != null) {
            s.setIdentityVerified(req.identityVerified());
        }
        if (req.recommendation() != null) {
            s.setRecommendation(req.recommendation());
        }
        if (req.notes() != null) {
            s.setNotes(req.notes());
        }
    }
}
