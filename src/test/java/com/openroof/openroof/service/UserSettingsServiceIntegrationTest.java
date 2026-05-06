package com.openroof.openroof.service;

import com.openroof.openroof.dto.settings.UpdateUserSettingsRequest;
import com.openroof.openroof.dto.settings.UserSettingsResponse;
import com.openroof.openroof.model.config.UserSettings;
import com.openroof.openroof.model.enums.NotifyChannel;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.UserRepository;
import com.openroof.openroof.repository.UserSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de integración para UserSettingsService.
 * Verifica creación automática, persistencia y actualización de ajustes en BD real (H2).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserSettingsServiceIntegrationTest {

    @Autowired UserSettingsService userSettingsService;
    @Autowired UserSettingsRepository userSettingsRepository;
    @Autowired UserRepository userRepository;

    private User user;
    private static final String USER_EMAIL = "user@integration.test";

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .email(USER_EMAIL)
                .name("Usuario Integración")
                .passwordHash("hashed")
                .role(UserRole.USER)
                .build());
    }

    @Nested
    @DisplayName("getSettings() — primer acceso")
    class FirstAccess {

        @Test
        @DisplayName("Crea una fila con defaults en BD al primer acceso")
        void autoCreatesRowWithAllDefaultsTrue() {
            assertThat(userSettingsRepository.findByUser(user)).isEmpty();

            UserSettingsResponse res = userSettingsService.getSettings(USER_EMAIL);

            assertThat(res.notifyPriceDrop()).isTrue();
            assertThat(res.notifyNewMatch()).isTrue();
            assertThat(res.notifyMessages()).isTrue();
            assertThat(res.notifyChannel()).isEqualTo(NotifyChannel.BOTH);
            assertThat(res.profileVisibleToAgents()).isTrue();
            assertThat(res.allowDirectContact()).isTrue();

            assertThat(userSettingsRepository.findByUser(user)).isPresent();
        }

        @Test
        @DisplayName("El canal por defecto guardado en BD es BOTH")
        void defaultChannelInDbIsBoth() {
            userSettingsService.getSettings(USER_EMAIL);

            UserSettings saved = userSettingsRepository.findByUser(user).orElseThrow();
            assertThat(saved.getNotifyChannel()).isEqualTo(NotifyChannel.BOTH);
        }

        @Test
        @DisplayName("No duplica la fila en accesos consecutivos")
        void noDuplicateRowOnRepeatedAccess() {
            userSettingsService.getSettings(USER_EMAIL);
            userSettingsService.getSettings(USER_EMAIL);

            assertThat(userSettingsRepository.findByUser(user)).isPresent();
        }
    }

    @Nested
    @DisplayName("updateSettings()")
    class UpdateSettings {

        @Test
        @DisplayName("Persiste el canal de notificación como string del enum en BD")
        void persistsNotifyChannelEnum() {
            userSettingsService.updateSettings(USER_EMAIL,
                    new UpdateUserSettingsRequest(true, true, true, NotifyChannel.EMAIL, true, true));

            UserSettings saved = userSettingsRepository.findByUser(user).orElseThrow();
            assertThat(saved.getNotifyChannel()).isEqualTo(NotifyChannel.EMAIL);
        }

        @Test
        @DisplayName("Persiste los ajustes de privacidad correctamente en BD")
        void persistsPrivacySettings() {
            userSettingsService.updateSettings(USER_EMAIL,
                    new UpdateUserSettingsRequest(true, true, true, NotifyChannel.BOTH, false, false));

            UserSettings saved = userSettingsRepository.findByUser(user).orElseThrow();
            assertThat(saved.isProfileVisibleToAgents()).isFalse();
            assertThat(saved.isAllowDirectContact()).isFalse();
        }

        @Test
        @DisplayName("Persiste los flags de notificación correctamente en BD")
        void persistsNotifyFlags() {
            userSettingsService.updateSettings(USER_EMAIL,
                    new UpdateUserSettingsRequest(false, false, true, NotifyChannel.IN_APP, true, true));

            UserSettings saved = userSettingsRepository.findByUser(user).orElseThrow();
            assertThat(saved.isNotifyPriceDrop()).isFalse();
            assertThat(saved.isNotifyNewMatch()).isFalse();
            assertThat(saved.isNotifyMessages()).isTrue();
        }

        @Test
        @DisplayName("getSettings() refleja los valores actualizados tras un update")
        void getAfterUpdate_returnsNewValues() {
            userSettingsService.updateSettings(USER_EMAIL,
                    new UpdateUserSettingsRequest(false, true, false, NotifyChannel.IN_APP, false, true));

            UserSettingsResponse res = userSettingsService.getSettings(USER_EMAIL);

            assertThat(res.notifyPriceDrop()).isFalse();
            assertThat(res.notifyNewMatch()).isTrue();
            assertThat(res.notifyMessages()).isFalse();
            assertThat(res.notifyChannel()).isEqualTo(NotifyChannel.IN_APP);
            assertThat(res.profileVisibleToAgents()).isFalse();
            assertThat(res.allowDirectContact()).isTrue();
        }

        @Test
        @DisplayName("Actualización consecutiva del canal — prevalece el último valor")
        void consecutiveChannelUpdates_lastValueWins() {
            userSettingsService.updateSettings(USER_EMAIL,
                    new UpdateUserSettingsRequest(true, true, true, NotifyChannel.EMAIL, true, true));
            userSettingsService.updateSettings(USER_EMAIL,
                    new UpdateUserSettingsRequest(true, true, true, NotifyChannel.IN_APP, true, true));

            UserSettings saved = userSettingsRepository.findByUser(user).orElseThrow();
            assertThat(saved.getNotifyChannel()).isEqualTo(NotifyChannel.IN_APP);
        }

        @Test
        @DisplayName("Dos usuarios distintos tienen sus propias filas de ajustes independientes")
        void twoUsersHaveIndependentSettings() {
            User user2 = userRepository.save(User.builder()
                    .email("user2@integration.test")
                    .name("Usuario 2")
                    .passwordHash("hashed")
                    .role(UserRole.USER)
                    .build());

            userSettingsService.updateSettings(USER_EMAIL,
                    new UpdateUserSettingsRequest(false, false, false, NotifyChannel.EMAIL, false, false));
            userSettingsService.updateSettings(user2.getEmail(),
                    new UpdateUserSettingsRequest(true, true, true, NotifyChannel.IN_APP, true, true));

            UserSettings settings1 = userSettingsRepository.findByUser(user).orElseThrow();
            UserSettings settings2 = userSettingsRepository.findByUser(user2).orElseThrow();

            assertThat(settings1.getNotifyChannel()).isEqualTo(NotifyChannel.EMAIL);
            assertThat(settings2.getNotifyChannel()).isEqualTo(NotifyChannel.IN_APP);
            assertThat(settings1.isProfileVisibleToAgents()).isFalse();
            assertThat(settings2.isProfileVisibleToAgents()).isTrue();
        }
    }
}
