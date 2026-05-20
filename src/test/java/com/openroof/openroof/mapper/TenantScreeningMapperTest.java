package com.openroof.openroof.mapper;

import com.openroof.openroof.dto.screening.TenantScreeningResponse;
import com.openroof.openroof.dto.screening.UpdateScreeningRequest;
import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.screening.TenantScreening;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@code TenantScreeningMapper}.
 */
@DisplayName("TenantScreeningMapper")
class TenantScreeningMapperTest {

    private final TenantScreeningMapper mapper = new TenantScreeningMapper();

    // ─── helpers ─────────────────────────────────────────────────────────────

    private TenantScreening buildScreening(Long id, Long applicationId) {
        RentalApplication application = null;
        if (applicationId != null) {
            application = mock(RentalApplication.class);
            when(application.getId()).thenReturn(applicationId);
        }

        LocalDateTime now = LocalDateTime.of(2026, 5, 19, 10, 0);
        TenantScreening s = TenantScreening.builder()
                .application(application)
                .provider(ScreeningProvider.MANUAL)
                .creditScore(720)
                .creditReportUrl("https://reports.test/credit")
                .backgroundCheckStatus(BackgroundCheckStatus.CLEAR)
                .backgroundReportUrl("https://reports.test/bg")
                .incomeVerified(true)
                .identityVerified(true)
                .recommendation(ScreeningRecommendation.APPROVE)
                .notes("Todo en orden")
                .expiresAt(now.plusMonths(3))
                .runAt(now)
                .build();
        s.setId(id);
        return s;
    }

    // ─── toResponse ──────────────────────────────────────────────────────────

    /**
     * toResponse maps all scalar fields verbatim.
     */
    @Test
    @DisplayName("toResponse maps all scalar fields verbatim")
    void toResponse_mapsAllScalarFields() {
        TenantScreening s = buildScreening(1L, 50L);

        TenantScreeningResponse resp = mapper.toResponse(s);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.applicationId()).isEqualTo(50L);
        assertThat(resp.provider()).isEqualTo(ScreeningProvider.MANUAL);
        assertThat(resp.creditScore()).isEqualTo(720);
        assertThat(resp.creditReportUrl()).isEqualTo("https://reports.test/credit");
        assertThat(resp.backgroundCheckStatus()).isEqualTo(BackgroundCheckStatus.CLEAR);
        assertThat(resp.backgroundReportUrl()).isEqualTo("https://reports.test/bg");
        assertThat(resp.incomeVerified()).isTrue();
        assertThat(resp.identityVerified()).isTrue();
        assertThat(resp.recommendation()).isEqualTo(ScreeningRecommendation.APPROVE);
        assertThat(resp.notes()).isEqualTo("Todo en orden");
    }

    /**
     * applicationId is null when screening has no application.
     */
    @Test
    @DisplayName("applicationId is null when screening has no application")
    void toResponse_noApplication_applicationIdIsNull() {
        TenantScreening s = buildScreening(2L, null);

        TenantScreeningResponse resp = mapper.toResponse(s);

        assertThat(resp.applicationId()).isNull();
    }

    /**
     * evictionHistory and criminalRecords are null when not set.
     */
    @Test
    @DisplayName("evictionHistory and criminalRecords are null when not set")
    void toResponse_noHistory_historyFieldsAreNull() {
        TenantScreening s = buildScreening(3L, null);

        TenantScreeningResponse resp = mapper.toResponse(s);

        assertThat(resp.evictionHistory()).isNull();
        assertThat(resp.criminalRecords()).isNull();
    }

    // ─── updateEntity ────────────────────────────────────────────────────────

    /**
     * updateEntity applies all non-null request fields.
     */
    @Test
    @DisplayName("updateEntity applies all non-null request fields")
    void updateEntity_allFieldsSet_allUpdated() {
        TenantScreening s = buildScreening(10L, 1L);
        UpdateScreeningRequest req = new UpdateScreeningRequest(
                580,
                BackgroundCheckStatus.FLAGGED,
                false,
                false,
                ScreeningRecommendation.REJECT,
                "Requiere revisión manual"
        );

        mapper.updateEntity(req, s);

        assertThat(s.getCreditScore()).isEqualTo(580);
        assertThat(s.getBackgroundCheckStatus()).isEqualTo(BackgroundCheckStatus.FLAGGED);
        assertThat(s.getIncomeVerified()).isFalse();
        assertThat(s.getIdentityVerified()).isFalse();
        assertThat(s.getRecommendation()).isEqualTo(ScreeningRecommendation.REJECT);
        assertThat(s.getNotes()).isEqualTo("Requiere revisión manual");
    }

    /**
     * updateEntity with all-null request leaves entity unchanged.
     */
    @Test
    @DisplayName("updateEntity with all-null request leaves entity unchanged")
    void updateEntity_allNullRequest_entityUnchanged() {
        TenantScreening s = buildScreening(10L, 1L);
        int originalScore = s.getCreditScore();
        BackgroundCheckStatus originalStatus = s.getBackgroundCheckStatus();

        UpdateScreeningRequest req = new UpdateScreeningRequest(null, null, null, null, null, null);
        mapper.updateEntity(req, s);

        assertThat(s.getCreditScore()).isEqualTo(originalScore);
        assertThat(s.getBackgroundCheckStatus()).isEqualTo(originalStatus);
        assertThat(s.getRecommendation()).isEqualTo(ScreeningRecommendation.APPROVE);
    }

    /**
     * updateEntity applies only the fields that are non-null in request.
     */
    @Test
    @DisplayName("updateEntity applies only the fields that are non-null in request")
    void updateEntity_partialRequest_onlyNonNullFieldsUpdated() {
        TenantScreening s = buildScreening(10L, 1L);
        UpdateScreeningRequest req = new UpdateScreeningRequest(650, null, null, null, null, "Nueva nota");

        mapper.updateEntity(req, s);

        assertThat(s.getCreditScore()).isEqualTo(650);
        assertThat(s.getNotes()).isEqualTo("Nueva nota");
        // fields not in request remain original
        assertThat(s.getBackgroundCheckStatus()).isEqualTo(BackgroundCheckStatus.CLEAR);
        assertThat(s.getRecommendation()).isEqualTo(ScreeningRecommendation.APPROVE);
    }
}
