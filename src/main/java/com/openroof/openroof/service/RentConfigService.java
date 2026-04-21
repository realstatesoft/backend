package com.openroof.openroof.service;

import com.openroof.openroof.dto.config.RentConfigResponse;
import com.openroof.openroof.dto.config.UpdateRentConfigRequest;
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
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentConfigService {

    public static final String KEY_DEPOSIT_MONTHS     = "RENT_DEPOSIT_MONTHS";
    public static final String KEY_COMMISSION_PERCENT = "RENT_COMMISSION_PERCENT";

    private static final int COMMISSION_SCALE = 2;

    private final SystemConfigRepository repo;

    public RentConfigResponse getRentConfig() {
        String depositRaw    = require(KEY_DEPOSIT_MONTHS).getConfigValue();
        String commissionRaw = require(KEY_COMMISSION_PERCENT).getConfigValue();
        try {
            int deposit           = Integer.parseInt(depositRaw);
            BigDecimal commission = new BigDecimal(commissionRaw);
            return new RentConfigResponse(deposit, commission);
        } catch (NumberFormatException e) {
            log.error("Invalid rent configuration in DB — {}='{}', {}='{}'",
                    KEY_DEPOSIT_MONTHS, depositRaw,
                    KEY_COMMISSION_PERCENT, commissionRaw, e);
            throw new InvalidConfigurationException("Invalid rent configuration", e);
        }
    }

    @Transactional
    public RentConfigResponse updateRentConfig(UpdateRentConfigRequest request) {
        BigDecimal normalizedCommission = request.commissionPercent()
                .setScale(COMMISSION_SCALE, RoundingMode.HALF_UP);

        SystemConfig deposit    = require(KEY_DEPOSIT_MONTHS);
        SystemConfig commission = require(KEY_COMMISSION_PERCENT);
        deposit.setConfigValue(String.valueOf(request.depositMonths()));
        commission.setConfigValue(normalizedCommission.toPlainString());
        repo.saveAll(List.of(deposit, commission));
        return new RentConfigResponse(request.depositMonths(), normalizedCommission);
    }

    private SystemConfig require(String key) {
        return repo.findByConfigKey(key)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Configuración no encontrada: " + key));
    }
}