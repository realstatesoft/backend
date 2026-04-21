package com.openroof.openroof.service;

import com.openroof.openroof.dto.config.RentConfigResponse;
import com.openroof.openroof.dto.property.RentCostBreakdownResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.repository.PropertyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RentCalculationService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final PropertyRepository propertyRepository;
    private final RentConfigService rentConfigService;

    public RentCostBreakdownResponse calculateInitialCost(Long propertyId) {
        Property property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new ResourceNotFoundException("Propiedad no encontrada"));

        PropertyCategory category = property.getCategory();
        if (category != PropertyCategory.RENT && category != PropertyCategory.SALE_OR_RENT) {
            throw new BadRequestException("La propiedad no está disponible para alquiler");
        }

        RentConfigResponse cfg = rentConfigService.getRentConfig();
        BigDecimal monthlyRent = property.getPrice().setScale(2, RoundingMode.HALF_UP);

        BigDecimal deposit = monthlyRent.multiply(BigDecimal.valueOf(cfg.depositMonths()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal firstMonth = monthlyRent;
        BigDecimal commission = monthlyRent.multiply(cfg.commissionPercent())
                .divide(HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal total = deposit.add(firstMonth).add(commission);

        return new RentCostBreakdownResponse(
                property.getId(),
                monthlyRent,
                cfg.depositMonths(),
                cfg.commissionPercent(),
                deposit,
                firstMonth,
                commission,
                total
        );
    }
}