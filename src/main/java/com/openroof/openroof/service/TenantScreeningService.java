package com.openroof.openroof.service;

import com.openroof.openroof.dto.screening.CreateScreeningRequest;
import com.openroof.openroof.dto.screening.TenantScreeningResponse;
import com.openroof.openroof.dto.screening.UpdateScreeningRequest;
import com.openroof.openroof.exception.BadRequestException;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Servicio de tenant screening para el provider INTERNAL (carga manual del admin).
 * Aplica reglas de negocio para recomendar APPROVE/REVIEW/REJECT.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantScreeningService {

    private static final int EXPIRATION_DAYS = 90;
    private static final BigDecimal MIN_INCOME_TO_RENT_RATIO = new BigDecimal("3");

    private final TenantScreeningRepository screeningRepository;
    private final RentalApplicationRepository rentalApplicationRepository;
    private final TenantScreeningMapper mapper;

    // ─── Lectura ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TenantScreeningResponse getById(Long id) {
        return mapper.toResponse(findScreening(id));
    }

    @Transactional(readOnly = true)
    public TenantScreeningResponse getByApplicationId(Long applicationId) {
        TenantScreening s = screeningRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TenantScreening", "applicationId", applicationId));
        return mapper.toResponse(s);
    }

    // ─── Creación ──────────────────────────────────────────────────────────────

    /**
     * Overload path-only: crea un screening a partir del applicationId.
     */
    @Transactional
    public TenantScreeningResponse createScreening(Long applicationId) {
        return createScreening(new CreateScreeningRequest(applicationId));
    }

    /**
     * Crea un screening interno para la rental application indicada.
     * Setea provider=INTERNAL, recommendation=REVIEW (placeholder hasta carga manual),
     * runAt=now y expiresAt=now+90d.
     */
    @Transactional
    public TenantScreeningResponse createScreening(CreateScreeningRequest req) {
        Long applicationId = req.applicationId();

        RentalApplication application = rentalApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "RentalApplication", "id", applicationId));

        if (screeningRepository.existsByApplicationId(applicationId)) {
            throw new BadRequestException(
                    "Screening already exists for application " + applicationId);
        }

        LocalDateTime now = LocalDateTime.now();
        TenantScreening screening = TenantScreening.builder()
                .application(application)
                .provider(ScreeningProvider.INTERNAL)
                .incomeVerified(false)
                .identityVerified(false)
                .recommendation(ScreeningRecommendation.REVIEW)
                .runAt(now)
                .expiresAt(now.plusDays(EXPIRATION_DAYS))
                .build();

        TenantScreening saved = screeningRepository.save(screening);
        log.info("Created internal screening id={} for application={}", saved.getId(), applicationId);
        return mapper.toResponse(saved);
    }

    // ─── Actualización manual ──────────────────────────────────────────────────

    /**
     * Aplica resultados manuales del admin. Si el DTO no trae recommendation,
     * la recalcula automáticamente con las reglas de negocio.
     * No modifica expiresAt (fijo desde la creación).
     */
    /**
     * Aplica resultados manuales resolviendo el screening por applicationId.
     */
    @Transactional
    public TenantScreeningResponse updateScreeningResultsByApplication(Long applicationId, UpdateScreeningRequest dto) {
        TenantScreening s = screeningRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TenantScreening", "applicationId", applicationId));
        return updateScreeningResults(s.getId(), dto);
    }

    @Transactional
    public TenantScreeningResponse updateScreeningResults(Long id, UpdateScreeningRequest dto) {
        TenantScreening screening = findScreening(id);

        mapper.updateEntity(dto, screening);

        if (dto.recommendation() == null) {
            applyRules(screening);
        }

        screening.setRunAt(LocalDateTime.now());

        TenantScreening saved = screeningRepository.save(screening);
        log.info("Updated screening id={} recommendation={}", saved.getId(), saved.getRecommendation());
        return mapper.toResponse(saved);
    }

    // ─── Recálculo explícito ───────────────────────────────────────────────────

    /**
     * Recalcula y persiste la recomendación según las reglas de negocio:
     *  1. evictions presentes ⇒ REJECT
     *  2. ratio>=3 y background=CLEAR ⇒ APPROVE
     *  3. resto ⇒ REVIEW
     */
    @Transactional
    public TenantScreeningResponse calculateRecommendation(Long id) {
        TenantScreening screening = findScreening(id);
        applyRules(screening);
        TenantScreening saved = screeningRepository.save(screening);
        return mapper.toResponse(saved);
    }

    // ─── Internos ──────────────────────────────────────────────────────────────

    private TenantScreening findScreening(Long id) {
        return screeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TenantScreening", "id", id));
    }

    /**
     * Aplica las reglas de negocio sobre la entidad sin persistir.
     */
    void applyRules(TenantScreening screening) {
        if (hasEvictions(screening)) {
            screening.setRecommendation(ScreeningRecommendation.REJECT);
            return;
        }

        BigDecimal ratio = resolveIncomeToRentRatio(screening.getApplication());
        boolean ratioOk = ratio != null && ratio.compareTo(MIN_INCOME_TO_RENT_RATIO) >= 0;
        boolean backgroundClear = screening.getBackgroundCheckStatus() == BackgroundCheckStatus.CLEAR;

        if (ratioOk && backgroundClear) {
            screening.setRecommendation(ScreeningRecommendation.APPROVE);
        } else {
            screening.setRecommendation(ScreeningRecommendation.REVIEW);
        }
    }

    private boolean hasEvictions(TenantScreening screening) {
        return screening.getEvictionHistory() != null
                && !screening.getEvictionHistory().isEmpty();
    }

    /**
     * Usa el ratio precomputado en la application; si está null, lo calcula desde
     * monthlyIncome y property.rentAmount. Devuelve null si faltan datos.
     */
    private BigDecimal resolveIncomeToRentRatio(RentalApplication application) {
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
