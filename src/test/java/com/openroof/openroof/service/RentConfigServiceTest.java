package com.openroof.openroof.service;

import com.openroof.openroof.dto.config.RentConfigResponse;
import com.openroof.openroof.dto.config.UpdateRentConfigRequest;
import com.openroof.openroof.exception.InvalidConfigurationException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.config.SystemConfig;
import com.openroof.openroof.repository.SystemConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentConfigServiceTest {

    @Mock private SystemConfigRepository repo;

    @InjectMocks
    private RentConfigService service;

    private SystemConfig cfg(String key, String val) {
        return SystemConfig.builder().configKey(key).configValue(val).build();
    }

    @Nested
    @DisplayName("getRentConfig()")
    class Get {
        @Test
        @DisplayName("Retorna meses y porcentaje desde la base")
        void returnsValues() {
            when(repo.findByConfigKey("RENT_DEPOSIT_MONTHS"))
                    .thenReturn(Optional.of(cfg("RENT_DEPOSIT_MONTHS", "2")));
            when(repo.findByConfigKey("RENT_COMMISSION_PERCENT"))
                    .thenReturn(Optional.of(cfg("RENT_COMMISSION_PERCENT", "5.5")));

            RentConfigResponse res = service.getRentConfig();

            assertThat(res.depositMonths()).isEqualTo(2);
            assertThat(res.commissionPercent()).isEqualByComparingTo("5.5");
        }

        @Test
        @DisplayName("Si falta una clave lanza ResourceNotFoundException")
        void missingKey_throws() {
            when(repo.findByConfigKey("RENT_DEPOSIT_MONTHS"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getRentConfig())
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Si el valor en BD no es parseable lanza InvalidConfigurationException sin filtrar el valor crudo")
        void invalidDbValue_throwsInvalidConfiguration() {
            when(repo.findByConfigKey("RENT_DEPOSIT_MONTHS"))
                    .thenReturn(Optional.of(cfg("RENT_DEPOSIT_MONTHS", "abc")));
            when(repo.findByConfigKey("RENT_COMMISSION_PERCENT"))
                    .thenReturn(Optional.of(cfg("RENT_COMMISSION_PERCENT", "5.5")));

            assertThatThrownBy(() -> service.getRentConfig())
                    .isInstanceOf(InvalidConfigurationException.class)
                    .hasMessage("Invalid rent configuration")
                    .hasMessageNotContaining("abc");
        }
    }

    @Nested
    @DisplayName("updateRentConfig()")
    class Update {
        @Test
        @DisplayName("Actualiza ambos valores y retorna el nuevo estado")
        void updatesBothValues() {
            SystemConfig depositRow = cfg("RENT_DEPOSIT_MONTHS", "1");
            SystemConfig commRow   = cfg("RENT_COMMISSION_PERCENT", "5.00");
            when(repo.findByConfigKey("RENT_DEPOSIT_MONTHS")).thenReturn(Optional.of(depositRow));
            when(repo.findByConfigKey("RENT_COMMISSION_PERCENT")).thenReturn(Optional.of(commRow));
            when(repo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            RentConfigResponse res = service.updateRentConfig(
                    new UpdateRentConfigRequest(3, new BigDecimal("7.25")));

            assertThat(res.depositMonths()).isEqualTo(3);
            assertThat(res.commissionPercent()).isEqualByComparingTo("7.25");

            ArgumentCaptor<List<SystemConfig>> captor = ArgumentCaptor.forClass(List.class);
            verify(repo).saveAll(captor.capture());
            assertThat(captor.getValue()).extracting(SystemConfig::getConfigValue)
                    .containsExactlyInAnyOrder("3", "7.25");
        }

        @Test
        @DisplayName("Normaliza commissionPercent a escala 2 HALF_UP antes de persistir y retornar")
        void normalizesCommissionScale() {
            SystemConfig depositRow = cfg("RENT_DEPOSIT_MONTHS", "1");
            SystemConfig commRow    = cfg("RENT_COMMISSION_PERCENT", "5.00");
            when(repo.findByConfigKey("RENT_DEPOSIT_MONTHS")).thenReturn(Optional.of(depositRow));
            when(repo.findByConfigKey("RENT_COMMISSION_PERCENT")).thenReturn(Optional.of(commRow));
            when(repo.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

            RentConfigResponse res = service.updateRentConfig(
                    new UpdateRentConfigRequest(2, new BigDecimal("7.255")));

            assertThat(res.commissionPercent()).isEqualByComparingTo("7.26");
            assertThat(res.commissionPercent().scale()).isEqualTo(2);

            ArgumentCaptor<List<SystemConfig>> captor = ArgumentCaptor.forClass(List.class);
            verify(repo).saveAll(captor.capture());
            assertThat(captor.getValue()).extracting(SystemConfig::getConfigValue)
                    .containsExactlyInAnyOrder("2", "7.26");
        }
    }
}