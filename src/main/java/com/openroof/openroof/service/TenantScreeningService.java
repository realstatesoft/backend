package com.openroof.openroof.service;

import com.openroof.openroof.dto.screening.CreateScreeningRequest;
import com.openroof.openroof.dto.screening.TenantScreeningResponse;
import com.openroof.openroof.dto.screening.UpdateScreeningRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.mapper.TenantScreeningMapper;
import com.openroof.openroof.model.rental.RentalApplication;
import com.openroof.openroof.model.screening.TenantScreening;
import com.openroof.openroof.repository.RentalApplicationRepository;
import com.openroof.openroof.repository.TenantScreeningRepository;
import com.openroof.openroof.screening.adapter.InternalScreeningAdapter;
import com.openroof.openroof.screening.adapter.ScreeningAdapterFactory;
import com.openroof.openroof.screening.adapter.ScreeningProviderAdapter;
import com.openroof.openroof.screening.adapter.ScreeningResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Servicio de tenant screening. Delega la corrida al adapter resuelto por
 * {@link ScreeningAdapterFactory}. El recálculo manual usa siempre el adapter
 * INTERNAL (reglas de negocio sobre los datos cargados por el admin).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantScreeningService {

    private static final int EXPIRATION_DAYS = 90;

    private final TenantScreeningRepository screeningRepository;
    private final RentalApplicationRepository rentalApplicationRepository;
    private final TenantScreeningMapper mapper;
    private final ScreeningAdapterFactory adapterFactory;
    private final InternalScreeningAdapter internalAdapter;

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

    @Transactional
    public TenantScreeningResponse createScreening(Long applicationId) {
        return createScreening(new CreateScreeningRequest(applicationId));
    }

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

        ScreeningProviderAdapter adapter = adapterFactory.resolveDefault();
        ScreeningResult result = adapter.runScreening(application);

        LocalDateTime now = LocalDateTime.now();
        TenantScreening screening = TenantScreening.builder()
                .application(application)
                .provider(result.provider())
                .creditScore(result.creditScore())
                .backgroundCheckStatus(result.backgroundCheckStatus())
                .evictionHistory(result.evictionHistory())
                .criminalRecords(result.criminalRecords())
                .incomeVerified(Boolean.TRUE.equals(result.incomeVerified()))
                .identityVerified(Boolean.TRUE.equals(result.identityVerified()))
                .recommendation(result.recommendation())
                .runAt(now)
                .expiresAt(now.plusDays(EXPIRATION_DAYS))
                .build();

        TenantScreening saved;
        try {
            saved = screeningRepository.saveAndFlush(screening);
        } catch (DataIntegrityViolationException ex) {
            log.debug("Concurrent screening insert for application={} rejected by UNIQUE constraint", applicationId);
            throw new BadRequestException(
                    "Screening already exists for application " + applicationId);
        }
        log.info("Created screening id={} provider={} for application={}",
                saved.getId(), saved.getProvider(), applicationId);
        return mapper.toResponse(saved);
    }

    // ─── Actualización manual ──────────────────────────────────────────────────

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
            screening.setRecommendation(internalAdapter.applyRules(screening));
        }

        screening.setRunAt(LocalDateTime.now());

        TenantScreening saved = screeningRepository.save(screening);
        log.info("Updated screening id={} recommendation={}", saved.getId(), saved.getRecommendation());
        return mapper.toResponse(saved);
    }

    // ─── Recálculo explícito ───────────────────────────────────────────────────

    @Transactional
    public TenantScreeningResponse calculateRecommendation(Long id) {
        TenantScreening screening = findScreening(id);
        screening.setRecommendation(internalAdapter.applyRules(screening));
        TenantScreening saved = screeningRepository.save(screening);
        return mapper.toResponse(saved);
    }

    // ─── Internos ──────────────────────────────────────────────────────────────

    private TenantScreening findScreening(Long id) {
        return screeningRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TenantScreening", "id", id));
    }
}
