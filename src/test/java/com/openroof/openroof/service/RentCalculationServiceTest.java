package com.openroof.openroof.service;

import com.openroof.openroof.dto.config.RentConfigResponse;
import com.openroof.openroof.dto.property.RentCostBreakdownResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.repository.PropertyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentCalculationServiceTest {

    @Mock private PropertyRepository propertyRepository;
    @Mock private RentConfigService rentConfigService;

    @InjectMocks
    private RentCalculationService service;

    private Property rentProperty(BigDecimal price) {
        Property p = Property.builder()
                .price(price)
                .category(PropertyCategory.RENT)
                .build();
        p.setId(10L);
        return p;
    }

    @Nested
    @DisplayName("calculateInitialCost()")
    class Calculate {
        @Test
        @DisplayName("Desglose correcto con 2 meses de depósito y 5% de comisión")
        void computesBreakdown() {
            when(propertyRepository.findById(10L))
                    .thenReturn(Optional.of(rentProperty(new BigDecimal("1000.00"))));
            when(rentConfigService.getRentConfig())
                    .thenReturn(new RentConfigResponse(2, new BigDecimal("5.00")));

            RentCostBreakdownResponse res = service.calculateInitialCost(10L);

            assertThat(res.propertyId()).isEqualTo(10L);
            assertThat(res.depositMonths()).isEqualTo(2);
            assertThat(res.commissionPercent()).isEqualByComparingTo("5.00");
            assertThat(res.deposit()).isEqualByComparingTo("2000.00");
            assertThat(res.firstMonth()).isEqualByComparingTo("1000.00");
            assertThat(res.commission()).isEqualByComparingTo("50.00");
            assertThat(res.total()).isEqualByComparingTo("3050.00");
        }

        @Test
        @DisplayName("Propiedad inexistente → ResourceNotFoundException")
        void propertyMissing_throws() {
            when(propertyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.calculateInitialCost(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Propiedad de venta (SALE) → BadRequestException")
        void nonRentProperty_throws() {
            Property sale = Property.builder()
                    .price(new BigDecimal("100000"))
                    .category(PropertyCategory.SALE)
                    .build();
            sale.setId(11L);
            when(propertyRepository.findById(11L)).thenReturn(Optional.of(sale));

            assertThatThrownBy(() -> service.calculateInitialCost(11L))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("alquiler");
        }

        @Test
        @DisplayName("SALE_OR_RENT también calcula el costo")
        void saleOrRent_ok() {
            Property p = Property.builder()
                    .price(new BigDecimal("500.00"))
                    .category(PropertyCategory.SALE_OR_RENT)
                    .build();
            p.setId(12L);
            when(propertyRepository.findById(12L)).thenReturn(Optional.of(p));
            when(rentConfigService.getRentConfig())
                    .thenReturn(new RentConfigResponse(1, new BigDecimal("10")));

            RentCostBreakdownResponse res = service.calculateInitialCost(12L);

            assertThat(res.deposit()).isEqualByComparingTo("500.00");
            assertThat(res.commission()).isEqualByComparingTo("50.00");
            assertThat(res.total()).isEqualByComparingTo("1050.00");
        }
    }
}