package com.openroof.openroof.service;

import com.openroof.openroof.dto.config.RentConfigResponse;
import com.openroof.openroof.dto.config.UpdateRentConfigRequest;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.config.SystemConfig;
import com.openroof.openroof.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentConfigService {

    public static final String KEY_DEPOSIT_MONTHS     = "RENT_DEPOSIT_MONTHS";
    public static final String KEY_COMMISSION_PERCENT = "RENT_COMMISSION_PERCENT";

    private final SystemConfigRepository repo;

    public RentConfigResponse getRentConfig() {
        String depositRaw    = require(KEY_DEPOSIT_MONTHS).getConfigValue();
        String commissionRaw = require(KEY_COMMISSION_PERCENT).getConfigValue();
        try {
            int deposit        = Integer.parseInt(depositRaw);
            BigDecimal commission = new BigDecimal(commissionRaw);
            return new RentConfigResponse(deposit, commission);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Invalid config value in DB — key='" + KEY_DEPOSIT_MONTHS +
                    "' value='" + depositRaw + "', key='" + KEY_COMMISSION_PERCENT +
                    "' value='" + commissionRaw + "'", e);
        }
    }

    @Transactional
    public RentConfigResponse updateRentConfig(UpdateRentConfigRequest request) {
        SystemConfig deposit = require(KEY_DEPOSIT_MONTHS);
        SystemConfig commission = require(KEY_COMMISSION_PERCENT);
        deposit.setConfigValue(String.valueOf(request.depositMonths()));
        commission.setConfigValue(request.commissionPercent().toPlainString());
        repo.saveAll(List.of(deposit, commission));
        return new RentConfigResponse(request.depositMonths(), request.commissionPercent());
    }

    private SystemConfig require(String key) {
        return repo.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Configuración no encontrada: " + key));
    }
}