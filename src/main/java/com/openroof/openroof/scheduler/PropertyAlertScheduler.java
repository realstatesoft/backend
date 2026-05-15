package com.openroof.openroof.scheduler;

import com.openroof.openroof.model.enums.AlertType;
import com.openroof.openroof.model.enums.PropertyStatus;
import com.openroof.openroof.model.enums.Visibility;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.search.Alert;
import com.openroof.openroof.model.search.SearchPreference;
import com.openroof.openroof.model.config.SystemConfig;
import com.openroof.openroof.repository.AlertRepository;
import com.openroof.openroof.repository.PropertyRepository;
import com.openroof.openroof.repository.SearchPreferenceRepository;
import com.openroof.openroof.repository.SystemConfigRepository;
import com.openroof.openroof.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class PropertyAlertScheduler {

    private final PropertyRepository propertyRepository;
    private final SearchPreferenceRepository searchPreferenceRepository;
    private final AlertRepository alertRepository;
    private final EmailService emailService;
    private final SystemConfigRepository systemConfigRepository;

    private static final String LAST_RUN_KEY = "scheduler.property-alerts.last-run";

    // Mapa de traducción para tipos de propiedad
    private static final Map<String, String> TYPE_MAP = Map.of(
            "casa", "HOUSE",
            "departamento", "APARTMENT",
            "terreno", "LAND",
            "oficina", "OFFICE",
            "deposito", "WAREHOUSE",
            "depósito", "WAREHOUSE",
            "quinta", "FARM");

    // Mapa de traducción para categorías
    private static final Map<String, String> CAT_MAP = Map.of(
            "venta", "SALE",
            "alquiler", "RENT",
            "venta o alquiler", "SALE_OR_RENT");

    /**
     * Busca propiedades nuevas y las matchea con las preferencias de los usuarios.
     * La frecuencia se configura en application.yml (por defecto cada hora).
     */
    @Scheduled(cron = "${application.scheduler.property-alerts.cron:0 0 * * * *}")
    @Transactional
    public void processPropertyAlerts() {
        log.info("Iniciando procesamiento de alertas de propiedades...");

        LocalDateTime lastRun = getLastRunWatermark();
        LocalDateTime currentRun = LocalDateTime.now();

        List<Property> newProperties = propertyRepository
                .findByDeletedAtIsNullAndStatusAndVisibilityAndCreatedAtAfterAndTrashedAtIsNull(
                        PropertyStatus.PUBLISHED, Visibility.PUBLIC, lastRun);

        if (newProperties.isEmpty()) {
            log.info("No hay propiedades nuevas desde la última ejecución ({}).", lastRun);
            updateLastRunWatermark(currentRun);
            return;
        }

        log.info("Se encontraron {} propiedades nuevas desde {}. Buscando matches...", newProperties.size(), lastRun);

        List<SearchPreference> activePreferences = searchPreferenceRepository.findByNotificationsEnabledTrue();

        int alertsCreated = 0;
        for (Property property : newProperties) {
            for (SearchPreference preference : activePreferences) {
                // No alertar al dueño de su propia propiedad
                if (property.getOwner().getId().equals(preference.getUser().getId())) {
                    log.debug("SALTEADO: El usuario ID {} es el dueño de la propiedad ID {}.",
                            preference.getUser().getId(), property.getId());
                    continue;
                }

                if (matches(property, preference)) {
                    createAlertAndNotify(property, preference);
                    alertsCreated++;
                }
            }
        }

        updateLastRunWatermark(currentRun);
        log.info("Procesamiento de alertas finalizado. {} alertas generadas.", alertsCreated);
    }

    private LocalDateTime getLastRunWatermark() {
        return systemConfigRepository.findByConfigKey(LAST_RUN_KEY)
                .map(config -> {
                    try {
                        return LocalDateTime.parse(config.getConfigValue());
                    } catch (Exception e) {
                        log.warn("Error parseando watermark '{}': {}. Usando default.", config.getConfigValue(),
                                e.getMessage());
                        return LocalDateTime.now().minusHours(1);
                    }
                })
                .orElse(LocalDateTime.now().minusHours(1));
    }

    private void updateLastRunWatermark(LocalDateTime timestamp) {
        SystemConfig config = systemConfigRepository.findByConfigKey(LAST_RUN_KEY)
                .orElse(SystemConfig.builder()
                        .configKey(LAST_RUN_KEY)
                        .build());
        config.setConfigValue(timestamp.toString());
        systemConfigRepository.save(config);
    }

    private boolean matches(Property prop, SearchPreference pref) {
        Map<String, Object> filters = pref.getFilters();
        if (filters == null || filters.isEmpty()) {
            log.debug("PREF DESCARTADA: La preferencia ID {} no tiene filtros configurados.", pref.getId());
            return false;
        }

        // 1. Tipo de Propiedad (con traducción)
        String prefType = (String) filters.get("propertyType");
        if (prefType != null && !prefType.isBlank()) {
            String translatedType = TYPE_MAP.getOrDefault(prefType.toLowerCase(), prefType.toUpperCase());
            if (!prop.getPropertyType().name().equalsIgnoreCase(translatedType)) {
                log.debug("DESCARTADO por TIPO: Propiedad es {} vs Preferencia {} (Traducción: {})",
                        prop.getPropertyType(), prefType, translatedType);
                return false;
            }
        }

        // 2. Categoría (con traducción)
        String prefCat = (String) filters.get("category");
        if (prefCat != null && !prefCat.isBlank()) {
            String translatedCat = CAT_MAP.getOrDefault(prefCat.toLowerCase(), prefCat.toUpperCase());
            if (!prop.getCategory().name().equalsIgnoreCase(translatedCat)) {
                log.debug("DESCARTADO por CATEGORÍA: Propiedad es {} vs Preferencia {} (Traducción: {})",
                        prop.getCategory(), prefCat, translatedCat);
                return false;
            }
        }

        // 3. Rango de Precio (Manejar Number o String)
        BigDecimal price = prop.getPrice();
        BigDecimal minPrice = parseBigDecimal(filters.get("minPrice"));
        BigDecimal maxPrice = parseBigDecimal(filters.get("maxPrice"));

        if (minPrice != null && price.compareTo(minPrice) < 0) {
            log.debug("DESCARTADO por PRECIO MÍN: Propiedad {} < Preferencia {}", price, minPrice);
            return false;
        }
        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            log.debug("DESCARTADO por PRECIO MÁX: Propiedad {} > Preferencia {}", price, maxPrice);
            return false;
        }

        // 4. Habitaciones (Manejar Number o String)
        Integer minBed = parseInteger(filters.get("minBedrooms"));
        if (minBed != null && prop.getBedrooms() < minBed) {
            log.debug("DESCARTADO por HABITACIONES: Propiedad {} < Preferencia {}", prop.getBedrooms(), minBed);
            return false;
        }

        // 5. Ciudad (Soportar 'city' o 'q' como fallback)
        String prefCity = (String) filters.get("city");
        if (prefCity == null || prefCity.isBlank()) {
            prefCity = (String) filters.get("q"); // El frontend usa 'q' para el buscador general
        }

        if (prefCity != null && !prefCity.isBlank()) {
            if (prop.getLocation() == null || prop.getLocation().getCity() == null
                    || prop.getLocation().getCity().isBlank()) {
                log.debug(
                        "DESCARTADO por CIUDAD: La preferencia requiere ciudad pero la propiedad no tiene ubicación o ciudad definida.");
                return false;
            }

            String pCity = normalize(prop.getLocation().getCity());
            String fCity = normalize(prefCity);

            if (pCity.isEmpty() || fCity.isEmpty() || (!pCity.contains(fCity) && !fCity.contains(pCity))) {
                log.debug("DESCARTADO por CIUDAD: Propiedad '{}' vs Preferencia '{}'", pCity, fCity);
                return false;
            }
        }

        log.info("MATCH ENCONTRADO: Propiedad ID {} coincide con preferencia ID {} del usuario ID {}",
                prop.getId(), pref.getId(), pref.getUser().getId());
        return true;
    }

    private BigDecimal parseBigDecimal(Object obj) {
        if (obj == null || obj.toString().isBlank())
            return null;
        try {
            return new BigDecimal(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(Object obj) {
        if (obj == null || obj.toString().isBlank())
            return null;
        try {
            return Integer.parseInt(obj.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private String normalize(String str) {
        if (str == null)
            return "";
        return str.toLowerCase()
                .replace("á", "a")
                .replace("é", "e")
                .replace("í", "i")
                .replace("ó", "o")
                .replace("ú", "u")
                .trim();
    }

    private void createAlertAndNotify(Property prop, SearchPreference pref) {
        // Verificar si ya existe una alerta para este match para evitar duplicados
        if (alertRepository.existsByUserIdAndPropertyIdAndSearchPreferenceId(
                pref.getUser().getId(), prop.getId(), pref.getId())) {
            return;
        }

        log.info("GENERANDO ALERTA: UserID: {} | PropertyID: {} | PreferenceID: {}",
                pref.getUser().getId(), prop.getId(), pref.getId());

        Alert alert = Alert.builder()
                .user(pref.getUser())
                .searchPreference(pref)
                .property(prop)
                .alertType(AlertType.NEW_MATCH)
                .title("¡Nueva propiedad encontrada!")
                .message("Se ha publicado una propiedad que coincide con tu búsqueda '" + pref.getName() + "': "
                        + prop.getTitle())
                .build();

        try {
            alertRepository.save(alert);
        } catch (DataIntegrityViolationException e) {
            log.info(
                    "ALERTA DUPLICADA (concurrente): Ya existe una alerta para UserID: {} | PropertyID: {} | PreferenceID: {}. Saltando.",
                    pref.getUser().getId(), prop.getId(), pref.getId());

            return;
        }

        // Enviar email asíncrono sólo tras el commit exitoso de la transacción
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    emailService.sendNewMatchAlertEmail(
                            pref.getUser().getEmail(),
                            pref.getUser().getName(),
                            prop.getTitle(),
                            prop.getPrice(),
                            prop.getId(),
                            pref.getName());
                }
            });
        } else {
            // Fallback si por alguna razón no hay transacción activa (aunque el método es
            // @Transactional)
            emailService.sendNewMatchAlertEmail(
                    pref.getUser().getEmail(),
                    pref.getUser().getName(),
                    prop.getTitle(),
                    prop.getPrice(),
                    prop.getId(),
                    pref.getName());
        }
    }
}
