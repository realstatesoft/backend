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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static com.openroof.openroof.service.AdminSettingsService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminSettingsServiceTest {

    @Mock private SystemConfigRepository repo;

    @InjectMocks
    private AdminSettingsService service;

    private SystemConfig cfg(String key, String val) {
        return SystemConfig.builder().configKey(key).configValue(val).build();
    }

    private void mockAllKeys() {
        when(repo.findByConfigKey(KEY_SALE_COMMISSION)).thenReturn(Optional.of(cfg(KEY_SALE_COMMISSION, "10.00")));
        when(repo.findByConfigKey(KEY_RENT_COMMISSION)).thenReturn(Optional.of(cfg(KEY_RENT_COMMISSION, "5.00")));
        when(repo.findByConfigKey(KEY_RENT_DEPOSIT)).thenReturn(Optional.of(cfg(KEY_RENT_DEPOSIT, "1")));
        when(repo.findByConfigKey(KEY_RESERVATION_TTL)).thenReturn(Optional.of(cfg(KEY_RESERVATION_TTL, "72")));
        when(repo.findByConfigKey(KEY_RESERVATION_DEPOSIT)).thenReturn(Optional.of(cfg(KEY_RESERVATION_DEPOSIT, "1.00")));
        when(repo.findByConfigKey(KEY_MAX_IMAGES)).thenReturn(Optional.of(cfg(KEY_MAX_IMAGES, "15")));
        when(repo.findByConfigKey(KEY_DEFAULT_CURRENCY)).thenReturn(Optional.of(cfg(KEY_DEFAULT_CURRENCY, "PYG")));
    }

    @Nested
    @DisplayName("getSettings()")
    class GetSettings {

        @Test
        @DisplayName("Retorna todos los valores correctamente agrupados por categoría")
        void returnsAllValuesMapped() {
            mockAllKeys();

            AdminSettingsResponse res = service.getSettings();

            assertThat(res.commissions().saleCommissionPercent()).isEqualByComparingTo("10.00");
            assertThat(res.commissions().rentCommissionPercent()).isEqualByComparingTo("5.00");
            assertThat(res.commissions().rentDepositMonths()).isEqualTo(1);
            assertThat(res.reservations().ttlHours()).isEqualTo(72);
            assertThat(res.reservations().depositPercent()).isEqualByComparingTo("1.00");
            assertThat(res.properties().maxImages()).isEqualTo(15);
            assertThat(res.system().defaultCurrency()).isEqualTo("PYG");
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si falta una clave en BD")
        void missingKey_throwsResourceNotFound() {
            when(repo.findByConfigKey(KEY_SALE_COMMISSION)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSettings())
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(KEY_SALE_COMMISSION);
        }

        @Test
        @DisplayName("Lanza InvalidConfigurationException si un valor no es parseable")
        void corruptIntValue_throwsInvalidConfiguration() {
            mockAllKeys();
            when(repo.findByConfigKey(KEY_RESERVATION_TTL))
                    .thenReturn(Optional.of(cfg(KEY_RESERVATION_TTL, "not-a-number")));

            assertThatThrownBy(() -> service.getSettings())
                    .isInstanceOf(InvalidConfigurationException.class);
        }

        @Test
        @DisplayName("Lanza InvalidConfigurationException si un decimal no es parseable")
        void corruptDecimalValue_throwsInvalidConfiguration() {
            mockAllKeys();
            when(repo.findByConfigKey(KEY_SALE_COMMISSION))
                    .thenReturn(Optional.of(cfg(KEY_SALE_COMMISSION, "abc")));

            assertThatThrownBy(() -> service.getSettings())
                    .isInstanceOf(InvalidConfigurationException.class);
        }
    }

    @Nested
    @DisplayName("updateCommissions()")
    class UpdateCommissions {

        @Test
        @DisplayName("Actualiza los 3 valores y devuelve el estado actualizado")
        void updatesThreeKeys() {
            mockAllKeys();
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AdminSettingsResponse res = service.updateCommissions(
                    new UpdateAdminCommissionsRequest(
                            new BigDecimal("12.00"), new BigDecimal("6.00"), 2));

            assertThat(res.commissions().saleCommissionPercent()).isEqualByComparingTo("12.00");
            assertThat(res.commissions().rentCommissionPercent()).isEqualByComparingTo("6.00");
            assertThat(res.commissions().rentDepositMonths()).isEqualTo(2);
            verify(repo, times(3)).save(any(SystemConfig.class));
        }

        @Test
        @DisplayName("Normaliza porcentajes a escala 2 HALF_UP antes de persistir")
        void normalizesPercentToScale2() {
            mockAllKeys();
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AdminSettingsResponse res = service.updateCommissions(
                    new UpdateAdminCommissionsRequest(
                            new BigDecimal("10.255"), new BigDecimal("5.005"), 1));

            assertThat(res.commissions().saleCommissionPercent()).isEqualByComparingTo("10.26");
            assertThat(res.commissions().rentCommissionPercent()).isEqualByComparingTo("5.01");
        }
    }

    @Nested
    @DisplayName("updateReservations()")
    class UpdateReservations {

        @Test
        @DisplayName("Actualiza TTL y depósito y devuelve los nuevos valores")
        void updatesTtlAndDeposit() {
            mockAllKeys();
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AdminSettingsResponse res = service.updateReservations(
                    new UpdateAdminReservationsRequest(48, new BigDecimal("5.00")));

            assertThat(res.reservations().ttlHours()).isEqualTo(48);
            assertThat(res.reservations().depositPercent()).isEqualByComparingTo("5.00");
            verify(repo, times(2)).save(any(SystemConfig.class));
        }
    }

    @Nested
    @DisplayName("updateProperties()")
    class UpdateProperties {

        @Test
        @DisplayName("Actualiza maxImages y devuelve el nuevo valor")
        void updatesMaxImages() {
            mockAllKeys();
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AdminSettingsResponse res = service.updateProperties(
                    new UpdateAdminPropertiesRequest(20));

            assertThat(res.properties().maxImages()).isEqualTo(20);
            verify(repo, times(1)).save(any(SystemConfig.class));
        }
    }

    @Nested
    @DisplayName("updateSystem()")
    class UpdateSystem {

        @Test
        @DisplayName("Actualiza la moneda y la retorna en mayúsculas")
        void updatesCurrencyUppercased() {
            mockAllKeys();
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            AdminSettingsResponse res = service.updateSystem(
                    new UpdateAdminSystemRequest("usd"));

            assertThat(res.system().defaultCurrency()).isEqualTo("USD");
            verify(repo, times(1)).save(any(SystemConfig.class));
        }
    }

    @Nested
    @DisplayName("getReservationTtlHours()")
    class GetReservationTtlHours {

        @Test
        @DisplayName("Retorna el valor entero del TTL de reservas")
        void returnsParsedInt() {
            when(repo.findByConfigKey(KEY_RESERVATION_TTL))
                    .thenReturn(Optional.of(cfg(KEY_RESERVATION_TTL, "72")));

            assertThat(service.getReservationTtlHours()).isEqualTo(72);
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si la clave no existe en BD")
        void missingKey_throwsResourceNotFound() {
            when(repo.findByConfigKey(KEY_RESERVATION_TTL)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getReservationTtlHours())
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Lanza InvalidConfigurationException si el valor no es un entero válido")
        void invalidValue_throwsInvalidConfiguration() {
            when(repo.findByConfigKey(KEY_RESERVATION_TTL))
                    .thenReturn(Optional.of(cfg(KEY_RESERVATION_TTL, "setenta-y-dos")));

            assertThatThrownBy(() -> service.getReservationTtlHours())
                    .isInstanceOf(InvalidConfigurationException.class);
        }
    }
}
