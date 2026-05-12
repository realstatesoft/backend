package com.openroof.openroof.service;

import com.openroof.openroof.dto.screening.TenantScreeningResponse;
import com.openroof.openroof.dto.screening.UpdateScreeningRequest;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.TenantScreeningMapper;
import com.openroof.openroof.model.enums.BackgroundCheckStatus;
import com.openroof.openroof.model.enums.ScreeningProvider;
import com.openroof.openroof.model.enums.ScreeningRecommendation;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.screening.TenantScreening;
import com.openroof.openroof.repository.RentalApplicationRepository;
import com.openroof.openroof.repository.TenantScreeningRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    // ─── Lectura ──────────────────────────────────────────────────────────────

    @Test
    void getById_found_returnsResponse() {
        Long id = 11L;
        TenantScreening s = TenantScreening.builder().build();
        s.setId(id);
        when(screeningRepository.findById(id)).thenReturn(Optional.of(s));
        when(mapper.toResponse(s)).thenReturn(new TenantScreeningResponse(
                id, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null));

        TenantScreeningResponse res = service.getById(id);

        assertNotNull(res);
        assertEquals(id, res.id());
    }

    @Test
    void getById_notFound_throwsResourceNotFound() {
        when(screeningRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getById(404L));
    }

    @Test
    void getByApplicationId_notFound_throwsResourceNotFound() {
        when(screeningRepository.findByApplicationId(404L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.getByApplicationId(404L));
    }

    // ─── createScreening: paths de error ──────────────────────────────────────

    @Test
    void createScreening_applicationNotFound_throwsResourceNotFound() {
        Long appId = 404L;
        when(rentalApplicationRepository.findById(appId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.createScreening(appId));
    }

    @Test
    void createScreening_concurrentRace_translatedToBadRequest() {
        Long appId = 66L;
        RentalApplication app = RentalApplication.builder().build();
        app.setId(appId);
        when(rentalApplicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(screeningRepository.existsByApplicationId(appId)).thenReturn(false);
        when(screeningRepository.saveAndFlush(any(TenantScreening.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException(
                        "duplicate key value violates unique constraint"));

        com.openroof.openroof.exception.BadRequestException ex = assertThrows(
                com.openroof.openroof.exception.BadRequestException.class,
                () -> service.createScreening(appId));

        org.junit.jupiter.api.Assertions.assertTrue(
                ex.getMessage().contains(String.valueOf(appId)),
                "El mensaje debe incluir el applicationId");
    }

    @Test
    void createScreening_duplicate_throwsBadRequest() {
        Long appId = 55L;
        RentalApplication app = RentalApplication.builder().build();
        app.setId(appId);
        when(rentalApplicationRepository.findById(appId)).thenReturn(Optional.of(app));
        when(screeningRepository.existsByApplicationId(appId)).thenReturn(true);

        assertThrows(com.openroof.openroof.exception.BadRequestException.class,
                () -> service.createScreening(appId));
    }

    // ─── calculateRecommendation ──────────────────────────────────────────────

    @Test
    void calculateRecommendation_appliesRulesAndSaves() {
        Long id = 200L;
        TenantScreening s = TenantScreening.builder()
                .application(buildApplication(new BigDecimal("4"), null, null))
                .backgroundCheckStatus(BackgroundCheckStatus.CLEAR)
                .build();
        s.setId(id);
        when(screeningRepository.findById(id)).thenReturn(Optional.of(s));
        when(screeningRepository.save(s)).thenReturn(s);
        when(mapper.toResponse(s)).thenReturn(new TenantScreeningResponse(
                id, null, null, null, null, null, null, null, null,
                null, null, s.getRecommendation(), null, null, null, null, null));

        TenantScreeningResponse res = service.calculateRecommendation(id);

        assertNotNull(res);
        assertEquals(ScreeningRecommendation.APPROVE, s.getRecommendation());
        verify(screeningRepository).save(s);
    }

    // ─── Overloads OR-231 ──────────────────────────────────────────────────────

    @Test
    void createScreening_byApplicationId_delegatesToDtoVariant() {
        Long applicationId = 77L;
        RentalApplication app = RentalApplication.builder().build();
        app.setId(applicationId);
        when(rentalApplicationRepository.findById(applicationId)).thenReturn(Optional.of(app));
        when(screeningRepository.existsByApplicationId(applicationId)).thenReturn(false);
        when(screeningRepository.saveAndFlush(any(TenantScreening.class)))
                .thenAnswer(inv -> {
                    TenantScreening s = inv.getArgument(0);
                    s.setId(123L);
                    return s;
                });
        when(mapper.toResponse(any(TenantScreening.class)))
                .thenAnswer(inv -> {
                    TenantScreening s = inv.getArgument(0);
                    return new TenantScreeningResponse(
                            s.getId(), applicationId, s.getProvider(), null, null, null, null,
                            null, null, s.getIncomeVerified(), s.getIdentityVerified(),
                            s.getRecommendation(), null, s.getExpiresAt(), s.getRunAt(), null, null);
                });

        TenantScreeningResponse res = service.createScreening(applicationId);

        assertNotNull(res);
        assertEquals(applicationId, res.applicationId());
        assertEquals(ScreeningProvider.INTERNAL, res.provider());
        assertEquals(ScreeningRecommendation.REVIEW, res.recommendation());

        ArgumentCaptor<TenantScreening> captor = ArgumentCaptor.forClass(TenantScreening.class);
        verify(screeningRepository).saveAndFlush(captor.capture());
        TenantScreening saved = captor.getValue();
        assertNotNull(saved.getRunAt());
        assertNotNull(saved.getExpiresAt());
        // expiresAt = runAt + 90 days
        assertEquals(saved.getRunAt().plusDays(90).toLocalDate(),
                saved.getExpiresAt().toLocalDate());
    }

    @Test
    void updateScreeningResultsByApplication_resolvesByApplicationId_andDelegates() {
        Long applicationId = 88L;
        Long screeningId = 555L;
        LocalDateTime originalRunAt = LocalDateTime.now().minusDays(1);
        TenantScreening existing = TenantScreening.builder()
                .application(buildApplication(new BigDecimal("4"), null, null))
                .runAt(originalRunAt)
                .expiresAt(LocalDateTime.now().plusDays(89))
                .build();
        existing.setId(screeningId);

        when(screeningRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(existing));
        when(screeningRepository.findById(screeningId)).thenReturn(Optional.of(existing));
        when(screeningRepository.save(any(TenantScreening.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(TenantScreening.class)))
                .thenAnswer(inv -> {
                    TenantScreening s = inv.getArgument(0);
                    return new TenantScreeningResponse(
                            s.getId(), applicationId, s.getProvider(), s.getCreditScore(),
                            null, s.getBackgroundCheckStatus(), null, null, null,
                            s.getIncomeVerified(), s.getIdentityVerified(),
                            s.getRecommendation(), s.getNotes(), s.getExpiresAt(),
                            s.getRunAt(), null, null);
                });

        UpdateScreeningRequest req = new UpdateScreeningRequest(
                720, BackgroundCheckStatus.CLEAR, true, true, null, "ok");

        TenantScreeningResponse res = service.updateScreeningResultsByApplication(applicationId, req);

        assertNotNull(res);
        assertEquals(screeningId, res.id());
        verify(screeningRepository).findByApplicationId(applicationId);
        verify(mapper).updateEntity(req, existing);
        verify(screeningRepository).save(existing);
        // runAt fue refrescado al ejecutar la actualización
        assertNotEquals(originalRunAt, existing.getRunAt());
    }

    @Test
    void updateScreeningResultsByApplication_applicationNotFound_throwsResourceNotFound() {
        Long applicationId = 99L;
        when(screeningRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        UpdateScreeningRequest req = new UpdateScreeningRequest(
                null, null, null, null, null, null);

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateScreeningResultsByApplication(applicationId, req));
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
