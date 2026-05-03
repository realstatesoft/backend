package com.openroof.openroof.service;

import com.openroof.openroof.dto.settings.AdminSettingsResponse;
import com.openroof.openroof.dto.settings.UpdateAdminCommissionsRequest;
import com.openroof.openroof.dto.settings.UpdateAdminPropertiesRequest;
import com.openroof.openroof.dto.settings.UpdateAdminReservationsRequest;
import com.openroof.openroof.dto.settings.UpdateAdminSystemRequest;
import com.openroof.openroof.exception.InvalidConfigurationException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.config.SystemConfig;
import com.openroof.openroof.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSettingsService {

    public static final String KEY_SALE_COMMISSION     = "SALE_COMMISSION_PERCENT";
    public static final String KEY_RENT_COMMISSION     = "RENT_COMMISSION_PERCENT";
    public static final String KEY_RENT_DEPOSIT        = "RENT_DEPOSIT_MONTHS";
    public static final String KEY_RESERVATION_TTL     = "RESERVATION_TTL_HOURS";
    public static final String KEY_RESERVATION_DEPOSIT = "RESERVATION_DEPOSIT_PERCENT";
    public static final String KEY_MAX_IMAGES          = "MAX_PROPERTY_IMAGES";
    public static final String KEY_DEFAULT_CURRENCY    = "DEFAULT_CURRENCY";

    private final SystemConfigRepository repo;

    public AdminSettingsResponse getSettings() {
        return new AdminSettingsResponse(
                buildCommissions(),
                buildReservations(),
                buildProperties(),
                buildSystem()
        );
    }

    @Transactional
    public AdminSettingsResponse updateCommissions(UpdateAdminCommissionsRequest req) {
        setScaled(KEY_SALE_COMMISSION, req.saleCommissionPercent());
        setScaled(KEY_RENT_COMMISSION, req.rentCommissionPercent());
        set(KEY_RENT_DEPOSIT, String.valueOf(req.rentDepositMonths()));
        return getSettings();
    }

    @Transactional
    public AdminSettingsResponse updateReservations(UpdateAdminReservationsRequest req) {
        set(KEY_RESERVATION_TTL, String.valueOf(req.ttlHours()));
        setScaled(KEY_RESERVATION_DEPOSIT, req.depositPercent());
        return getSettings();
    }

    @Transactional
    public AdminSettingsResponse updateProperties(UpdateAdminPropertiesRequest req) {
        set(KEY_MAX_IMAGES, String.valueOf(req.maxImages()));
        return getSettings();
    }

    @Transactional
    public AdminSettingsResponse updateSystem(UpdateAdminSystemRequest req) {
        set(KEY_DEFAULT_CURRENCY, req.defaultCurrency().toUpperCase());
        return getSettings();
    }

    public int getReservationTtlHours() {
        return parseInt(require(KEY_RESERVATION_TTL));
    }

    private AdminSettingsResponse.CommissionSettings buildCommissions() {
        return new AdminSettingsResponse.CommissionSettings(
                parseDecimal(require(KEY_SALE_COMMISSION)),
                parseDecimal(require(KEY_RENT_COMMISSION)),
                parseInt(require(KEY_RENT_DEPOSIT))
        );
    }

    private AdminSettingsResponse.ReservationSettings buildReservations() {
        return new AdminSettingsResponse.ReservationSettings(
                parseInt(require(KEY_RESERVATION_TTL)),
                parseDecimal(require(KEY_RESERVATION_DEPOSIT))
        );
    }

    private AdminSettingsResponse.PropertySettings buildProperties() {
        return new AdminSettingsResponse.PropertySettings(
                parseInt(require(KEY_MAX_IMAGES))
        );
    }

    private AdminSettingsResponse.SystemSettings buildSystem() {
        return new AdminSettingsResponse.SystemSettings(
                require(KEY_DEFAULT_CURRENCY).getConfigValue()
        );
    }

    private void set(String key, String value) {
        SystemConfig config = require(key);
        config.setConfigValue(value);
        repo.save(config);
    }

    private void setScaled(String key, BigDecimal value) {
        set(key, value.setScale(2, RoundingMode.HALF_UP).toPlainString());
    }

    private SystemConfig require(String key) {
        return repo.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Configuración no encontrada: " + key));
    }

    private int parseInt(SystemConfig config) {
        try {
            return Integer.parseInt(config.getConfigValue());
        } catch (NumberFormatException e) {
            throw new InvalidConfigurationException(
                    "Valor inválido para " + config.getConfigKey() + ": " + config.getConfigValue(), e);
        }
    }

    private BigDecimal parseDecimal(SystemConfig config) {
        try {
            return new BigDecimal(config.getConfigValue());
        } catch (NumberFormatException e) {
            throw new InvalidConfigurationException(
                    "Valor inválido para " + config.getConfigKey() + ": " + config.getConfigValue(), e);
        }
    }
}
