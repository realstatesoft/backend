package com.openroof.openroof.service;

import com.openroof.openroof.dto.settings.AdminSettingsResponse;
import com.openroof.openroof.dto.settings.UpdateAdminCommissionsRequest;
import com.openroof.openroof.dto.settings.UpdateAdminPropertiesRequest;
import com.openroof.openroof.dto.settings.UpdateAdminReservationsRequest;
import com.openroof.openroof.dto.settings.UpdateAdminSystemRequest;
import com.openroof.openroof.model.config.SystemConfig;
import com.openroof.openroof.repository.SystemConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static com.openroof.openroof.service.AdminSettingsService.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para AdminSettingsService.
 * Verifica que los valores se leen y persisten correctamente en BD real (H2).
 */
@SpringBootTest
@Transactional
class AdminSettingsServiceIntegrationTest {

    @Autowired AdminSettingsService adminSettingsService;
    @Autowired SystemConfigRepository systemConfigRepository;

    @BeforeEach
    void setUp() {
        systemConfigRepository.deleteAll();
        systemConfigRepository.saveAll(List.of(
                SystemConfig.builder().configKey(KEY_SALE_COMMISSION).configValue("10.00").build(),
                SystemConfig.builder().configKey(KEY_RENT_COMMISSION).configValue("5.00").build(),
                SystemConfig.builder().configKey(KEY_RENT_DEPOSIT).configValue("1").build(),
                SystemConfig.builder().configKey(KEY_RESERVATION_TTL).configValue("72").build(),
                SystemConfig.builder().configKey(KEY_RESERVATION_DEPOSIT).configValue("1.00").build(),
                SystemConfig.builder().configKey(KEY_MAX_IMAGES).configValue("15").build(),
                SystemConfig.builder().configKey(KEY_DEFAULT_CURRENCY).configValue("PYG").build()
        ));
    }

    @Nested
    @DisplayName("getSettings()")
    class GetSettings {

        @Test
        @DisplayName("Lee todos los valores correctamente desde BD real")
        void returnsAllValuesFromDb() {
            AdminSettingsResponse res = adminSettingsService.getSettings();

            assertThat(res.commissions().saleCommissionPercent()).isEqualByComparingTo("10.00");
            assertThat(res.commissions().rentCommissionPercent()).isEqualByComparingTo("5.00");
            assertThat(res.commissions().rentDepositMonths()).isEqualTo(1);
            assertThat(res.reservations().ttlHours()).isEqualTo(72);
            assertThat(res.reservations().depositPercent()).isEqualByComparingTo("1.00");
            assertThat(res.properties().maxImages()).isEqualTo(15);
            assertThat(res.system().defaultCurrency()).isEqualTo("PYG");
        }
    }

    @Nested
    @DisplayName("updateCommissions()")
    class UpdateCommissions {

        @Test
        @DisplayName("Persiste los nuevos porcentajes y meses en BD")
        void persistsNewValues() {
            adminSettingsService.updateCommissions(
                    new UpdateAdminCommissionsRequest(
                            new BigDecimal("12.50"), new BigDecimal("7.00"), 3));

            AdminSettingsResponse res = adminSettingsService.getSettings();

            assertThat(res.commissions().saleCommissionPercent()).isEqualByComparingTo("12.50");
            assertThat(res.commissions().rentCommissionPercent()).isEqualByComparingTo("7.00");
            assertThat(res.commissions().rentDepositMonths()).isEqualTo(3);
        }

        @Test
        @DisplayName("Normaliza a escala 2 antes de persistir")
        void persistsNormalizedScale() {
            adminSettingsService.updateCommissions(
                    new UpdateAdminCommissionsRequest(
                            new BigDecimal("10.999"), new BigDecimal("5.005"), 1));

            SystemConfig saleRow = systemConfigRepository
                    .findByConfigKey(KEY_SALE_COMMISSION).orElseThrow();
            SystemConfig rentRow = systemConfigRepository
                    .findByConfigKey(KEY_RENT_COMMISSION).orElseThrow();

            assertThat(saleRow.getConfigValue()).isEqualTo("11.00");
            assertThat(rentRow.getConfigValue()).isEqualTo("5.01");
        }

        @Test
        @DisplayName("Actualización consecutiva — prevalece el último valor guardado")
        void consecutiveUpdatesPersistLastValue() {
            adminSettingsService.updateCommissions(
                    new UpdateAdminCommissionsRequest(new BigDecimal("8.00"), new BigDecimal("4.00"), 2));
            adminSettingsService.updateCommissions(
                    new UpdateAdminCommissionsRequest(new BigDecimal("15.00"), new BigDecimal("6.00"), 4));

            AdminSettingsResponse res = adminSettingsService.getSettings();

            assertThat(res.commissions().saleCommissionPercent()).isEqualByComparingTo("15.00");
            assertThat(res.commissions().rentDepositMonths()).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("updateReservations()")
    class UpdateReservations {

        @Test
        @DisplayName("Persiste TTL y porcentaje de depósito en BD")
        void persistsTtlAndDeposit() {
            adminSettingsService.updateReservations(
                    new UpdateAdminReservationsRequest(48, new BigDecimal("5.00")));

            AdminSettingsResponse res = adminSettingsService.getSettings();

            assertThat(res.reservations().ttlHours()).isEqualTo(48);
            assertThat(res.reservations().depositPercent()).isEqualByComparingTo("5.00");
        }

        @Test
        @DisplayName("El valor del TTL en BD es exactamente el string del entero")
        void ttlStoredAsIntegerString() {
            adminSettingsService.updateReservations(
                    new UpdateAdminReservationsRequest(120, new BigDecimal("10.00")));

            SystemConfig ttlRow = systemConfigRepository
                    .findByConfigKey(KEY_RESERVATION_TTL).orElseThrow();

            assertThat(ttlRow.getConfigValue()).isEqualTo("120");
        }
    }

    @Nested
    @DisplayName("updateProperties()")
    class UpdateProperties {

        @Test
        @DisplayName("Persiste el límite de imágenes en BD")
        void persistsMaxImages() {
            adminSettingsService.updateProperties(new UpdateAdminPropertiesRequest(25));

            SystemConfig row = systemConfigRepository
                    .findByConfigKey(KEY_MAX_IMAGES).orElseThrow();

            assertThat(row.getConfigValue()).isEqualTo("25");
            assertThat(adminSettingsService.getSettings().properties().maxImages()).isEqualTo(25);
        }
    }

    @Nested
    @DisplayName("updateSystem()")
    class UpdateSystem {

        @Test
        @DisplayName("Persiste la moneda en mayúsculas en BD")
        void persistsCurrencyUppercased() {
            adminSettingsService.updateSystem(new UpdateAdminSystemRequest("usd"));

            SystemConfig row = systemConfigRepository
                    .findByConfigKey(KEY_DEFAULT_CURRENCY).orElseThrow();

            assertThat(row.getConfigValue()).isEqualTo("USD");
            assertThat(adminSettingsService.getSettings().system().defaultCurrency()).isEqualTo("USD");
        }
    }

    @Nested
    @DisplayName("getReservationTtlHours()")
    class GetReservationTtlHours {

        @Test
        @DisplayName("Lee el TTL de reservas directamente desde BD")
        void returnsTtlFromDb() {
            assertThat(adminSettingsService.getReservationTtlHours()).isEqualTo(72);
        }

        @Test
        @DisplayName("Refleja actualizaciones previas sin consultar caché")
        void reflectsUpdatedValue() {
            adminSettingsService.updateReservations(
                    new UpdateAdminReservationsRequest(96, new BigDecimal("2.00")));

            assertThat(adminSettingsService.getReservationTtlHours()).isEqualTo(96);
        }
    }
}
