package com.openroof.openroof.service;

import com.openroof.openroof.dto.settings.UpdateUserSettingsRequest;
import com.openroof.openroof.dto.settings.UserSettingsResponse;
import com.openroof.openroof.exception.ResourceNotFoundException;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTest {

    @Mock private UserSettingsRepository repo;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private UserSettingsService service;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().name("Comprador").email("user@test.com").role(UserRole.USER).build();
        user.setId(1L);
    }

    private UserSettings existingSettings() {
        UserSettings s = UserSettings.builder()
                .user(user)
                .notifyPriceDrop(false)
                .notifyNewMatch(true)
                .notifyMessages(false)
                .notifyChannel(NotifyChannel.EMAIL)
                .profileVisibleToAgents(false)
                .allowDirectContact(true)
                .build();
        s.setId(10L);
        return s;
    }

    @Nested
    @DisplayName("getSettings()")
    class GetSettings {

        @Test
        @DisplayName("Retorna los ajustes existentes del usuario")
        void returnsExistingSettings() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(repo.findByUser(user)).thenReturn(Optional.of(existingSettings()));

            UserSettingsResponse res = service.getSettings("user@test.com");

            assertThat(res.notifyPriceDrop()).isFalse();
            assertThat(res.notifyNewMatch()).isTrue();
            assertThat(res.notifyMessages()).isFalse();
            assertThat(res.notifyChannel()).isEqualTo(NotifyChannel.EMAIL);
            assertThat(res.profileVisibleToAgents()).isFalse();
            assertThat(res.allowDirectContact()).isTrue();
        }

        @Test
        @DisplayName("Auto-crea ajustes con defaults cuando el usuario no tiene registro previo")
        void autoCreatesWithDefaultsOnFirstAccess() {
            UserSettings defaults = UserSettings.builder().user(user).build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(repo.findByUser(user)).thenReturn(Optional.empty());
            when(repo.save(any())).thenReturn(defaults);

            UserSettingsResponse res = service.getSettings("user@test.com");

            assertThat(res.notifyPriceDrop()).isTrue();
            assertThat(res.notifyNewMatch()).isTrue();
            assertThat(res.notifyMessages()).isTrue();
            assertThat(res.notifyChannel()).isEqualTo(NotifyChannel.BOTH);
            assertThat(res.profileVisibleToAgents()).isTrue();
            assertThat(res.allowDirectContact()).isTrue();
            verify(repo).save(any(UserSettings.class));
        }

        @Test
        @DisplayName("Lanza ResourceNotFoundException si el usuario no existe")
        void userNotFound_throwsResourceNotFound() {
            when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getSettings("ghost@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateSettings()")
    class UpdateSettings {

        @Test
        @DisplayName("Actualiza todos los campos incluyendo el canal de notificación")
        void updatesAllFields() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(repo.findByUser(user)).thenReturn(Optional.of(existingSettings()));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            UserSettingsResponse res = service.updateSettings("user@test.com",
                    new UpdateUserSettingsRequest(true, false, true, NotifyChannel.IN_APP, false, false));

            assertThat(res.notifyPriceDrop()).isTrue();
            assertThat(res.notifyNewMatch()).isFalse();
            assertThat(res.notifyMessages()).isTrue();
            assertThat(res.notifyChannel()).isEqualTo(NotifyChannel.IN_APP);
            assertThat(res.profileVisibleToAgents()).isFalse();
            assertThat(res.allowDirectContact()).isFalse();
        }

        @Test
        @DisplayName("Persiste la entidad con el canal actualizado a través del repositorio")
        void callsRepoSaveWithUpdatedChannel() {
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(repo.findByUser(user)).thenReturn(Optional.of(existingSettings()));
            when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.updateSettings("user@test.com",
                    new UpdateUserSettingsRequest(true, true, true, NotifyChannel.BOTH, true, true));

            ArgumentCaptor<UserSettings> captor = ArgumentCaptor.forClass(UserSettings.class);
            verify(repo).save(captor.capture());
            assertThat(captor.getValue().getNotifyChannel()).isEqualTo(NotifyChannel.BOTH);
        }

        @Test
        @DisplayName("Crea y actualiza si el usuario accede por primera vez")
        void createsAndUpdatesOnFirstAccess() {
            UserSettings blank = UserSettings.builder().user(user).build();
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
            when(repo.findByUser(user)).thenReturn(Optional.empty());
            when(repo.save(any())).thenReturn(blank).thenAnswer(inv -> inv.getArgument(0));

            UserSettingsResponse res = service.updateSettings("user@test.com",
                    new UpdateUserSettingsRequest(false, false, false, NotifyChannel.EMAIL, false, false));

            assertThat(res.notifyChannel()).isEqualTo(NotifyChannel.EMAIL);
        }
    }
}
