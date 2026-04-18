package com.openroof.openroof.service;

import com.openroof.openroof.dto.flag.CreateFlagRequest;
import com.openroof.openroof.dto.flag.FlagResponse;
import com.openroof.openroof.dto.flag.FlagSummaryResponse;
import com.openroof.openroof.dto.flag.ResolveFlagRequest;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.admin.PropertyFlag;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.PropertyFlagRepository;
import com.openroof.openroof.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PropertyFlagService {

    private final PropertyFlagRepository propertyFlagRepository;
    private final PropertyRepository propertyRepository;

    // ─── CREATE ───────────────────────────────────────────────────

    /**
     * Crea un nuevo reporte (flag) para una propiedad.
     * Comprueba que la propiedad exista y que el usuario no haya reportado ya la misma propiedad.
     */
    public FlagResponse createFlag(Long propertyId, CreateFlagRequest request, User currentUser) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Propiedad no encontrada con ID: " + propertyId));

        // Anti-spam: un usuario no puede reportar la misma propiedad dos veces (check-then-insert)
        propertyFlagRepository
                .findByPropertyIdAndReportedByIdAndResolvedAtIsNull(propertyId, currentUser.getId())
                .ifPresent(existing -> {
                    throw new BadRequestException("Ya has reportado esta propiedad");
                });

        PropertyFlag flag = PropertyFlag.builder()
                .property(property)
                .reportedBy(currentUser)
                .flagType(request.flagType())
                .reason(request.reason())
                .build();

        try {
            PropertyFlag saved = propertyFlagRepository.save(flag);
            // Asegurar que el flush ocurra dentro del try-catch para capturar violaciones de constraint
            propertyFlagRepository.flush();
            log.info("Propiedad {} reportada por usuario {} como {}",
                    propertyId, currentUser.getId(), request.flagType());
            return toResponse(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Intento de reporte duplicado detectado por constraint de BD: property={}, user={}",
                    propertyId, currentUser.getId());
            throw new BadRequestException("Ya has reportado esta propiedad");
        }
    }

    // ─── READ ─────────────────────────────────────────────────────

    /** Devuelve todos los flags activos (no resueltos) de una propiedad. Publico (sin PII). */
    @Transactional(readOnly = true)
    public List<FlagSummaryResponse> getActiveFlagsByProperty(Long propertyId) {
        return propertyFlagRepository
                .findByPropertyIdAndResolvedAtIsNull(propertyId)
                .stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    /** Devuelve la cantidad de flags activos de una propiedad. */
    @Transactional(readOnly = true)
    public long countActiveFlags(Long propertyId) {
        return propertyFlagRepository.countByPropertyIdAndResolvedAtIsNull(propertyId);
    }

    /** Devuelve todos los flags activos del sistema (uso exclusivo ADMIN). */
    @Transactional(readOnly = true)
    public List<FlagResponse> getAllActiveFlags() {
        return propertyFlagRepository
                .findAllByResolvedAtIsNull()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── RESOLVE ──────────────────────────────────────────────────

    /**
     * Resuelve un flag: establece resolvedAt, resolvedBy y resolutionNotes.
     * Solo debe ser invocado por un administrador.
     */
    public FlagResponse resolveFlag(Long flagId, ResolveFlagRequest request, User admin) {
        PropertyFlag flag = propertyFlagRepository.findById(flagId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Reporte no encontrado con ID: " + flagId));

        if (flag.isResolved()) {
            throw new BadRequestException("Este reporte ya fue resuelto");
        }

        flag.setResolvedAt(LocalDateTime.now());
        flag.setResolvedBy(admin);
        flag.setResolutionNotes(request.resolutionNotes());

        PropertyFlag saved = propertyFlagRepository.save(flag);
        log.info("Reporte {} resuelto por admin {}", flagId, admin.getId());

        return toResponse(saved);
    }

    // ─── Private helpers ──────────────────────────────────────────

    private FlagResponse toResponse(PropertyFlag flag) {
        return new FlagResponse(
                flag.getId(),
                flag.getProperty().getId(),
                flag.getFlagType(),
                flag.getReason(),
                flag.getReportedBy().getUsername(),  // getUsername() devuelve el email
                flag.getCreatedAt(),
                flag.getResolvedAt(),
                flag.getResolutionNotes()
        );
    }

    private FlagSummaryResponse toSummaryResponse(PropertyFlag flag) {
        return new FlagSummaryResponse(
                flag.getId(),
                flag.getProperty().getId(),
                flag.getFlagType(),
                flag.getReason(),
                flag.getCreatedAt(),
                flag.getResolvedAt()
        );
    }
}
