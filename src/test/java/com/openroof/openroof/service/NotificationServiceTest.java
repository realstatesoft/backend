package com.openroof.openroof.service;

import com.openroof.openroof.dto.notification.CreateNotificationRequest;
import com.openroof.openroof.dto.notification.NotificationResponse;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.NotificationType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.notification.Notification;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.NotificationRepository;
import com.openroof.openroof.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Nested
    @DisplayName("create()")
    class CreateTests {

        @Test
        @DisplayName("Usuario autenticado crea notificación para sí mismo")
        void createForCurrentUser_returnsResponse() {
            User currentUser = user(10L, "user@test.com", UserRole.USER);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
                Notification notification = invocation.getArgument(0);
                notification.setId(99L);
                notification.setCreatedAt(LocalDateTime.of(2026, 3, 29, 12, 0));
                notification.setUpdatedAt(LocalDateTime.of(2026, 3, 29, 12, 0));
                return notification;
            });

            CreateNotificationRequest request = new CreateNotificationRequest(
                    null,
                    NotificationType.VISIT,
                    " Nueva solicitud de visita ",
                    " Tenés una visita pendiente ",
                    Map.of("visitRequestId", 44),
                    " /visit-requests "
            );

            NotificationResponse response = notificationService.create(request, "user@test.com");

            assertThat(response.id()).isEqualTo(99L);
            assertThat(response.userId()).isEqualTo(10L);
            assertThat(response.type()).isEqualTo(NotificationType.VISIT);
            assertThat(response.title()).isEqualTo("Nueva solicitud de visita");
            assertThat(response.message()).isEqualTo("Tenés una visita pendiente");
            assertThat(response.actionUrl()).isEqualTo("/visit-requests");
            assertThat(response.read()).isFalse();
        }

        @Test
        @DisplayName("ADMIN puede crear notificación para otro usuario")
        void createForAnotherUserAsAdmin_returnsResponse() {
            User admin = user(1L, "admin@test.com", UserRole.ADMIN);
            User targetUser = user(2L, "target@test.com", UserRole.USER);
            when(userRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
            when(userRepository.findById(2L)).thenReturn(Optional.of(targetUser));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
                Notification notification = invocation.getArgument(0);
                notification.setId(50L);
                notification.setCreatedAt(LocalDateTime.now());
                notification.setUpdatedAt(LocalDateTime.now());
                return notification;
            });

            CreateNotificationRequest request = new CreateNotificationRequest(
                    2L,
                    NotificationType.SYSTEM,
                    "Perfil actualizado",
                    "Tus datos se actualizaron correctamente",
                    Map.of("userId", 2),
                    "/profile"
            );

            NotificationResponse response = notificationService.create(request, "admin@test.com");

            assertThat(response.userId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("Usuario no admin no puede crear notificación para otro usuario")
        void createForAnotherUserAsNonAdmin_throwsForbidden() {
            User currentUser = user(10L, "user@test.com", UserRole.USER);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));

            CreateNotificationRequest request = new CreateNotificationRequest(
                    25L,
                    NotificationType.ALERT,
                    "Recordatorio",
                    "Tenés una tarea pendiente",
                    null,
                    null
            );

            assertThatThrownBy(() -> notificationService.create(request, "user@test.com"))
                    .isInstanceOf(ForbiddenException.class)
                    .hasMessageContaining("ADMIN");
            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getMyNotifications()")
    class GetMyNotificationsTests {

        @Test
        @DisplayName("Lista las notificaciones del usuario actual")
        void getMyNotifications_returnsMappedResponses() {
            User currentUser = user(10L, "user@test.com", UserRole.USER);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
            when(notificationRepository.findByUser_IdOrderByCreatedAtDesc(10L)).thenReturn(List.of(
                    notification(1L, currentUser, NotificationType.VISIT, null),
                    notification(2L, currentUser, NotificationType.ALERT, LocalDateTime.of(2026, 3, 28, 8, 0))
            ));

            List<NotificationResponse> responses = notificationService.getMyNotifications("user@test.com");

            assertThat(responses).hasSize(2);
            assertThat(responses.getFirst().read()).isFalse();
            assertThat(responses.get(1).read()).isTrue();
        }
    }

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsReadTests {

        @Test
        @DisplayName("Marca como leída una notificación no leída")
        void markAsRead_whenUnread_updatesReadAt() {
            User currentUser = user(10L, "user@test.com", UserRole.USER);
            Notification notification = notification(1L, currentUser, NotificationType.VISIT, null);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
            when(notificationRepository.findByIdAndUser_Id(1L, 10L)).thenReturn(Optional.of(notification));
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            NotificationResponse response = notificationService.markAsRead(1L, "user@test.com");

            assertThat(response.read()).isTrue();
            assertThat(response.readAt()).isNotNull();
        }

        @Test
        @DisplayName("Si ya estaba leída no vuelve a persistir")
        void markAsRead_whenAlreadyRead_doesNotSaveAgain() {
            User currentUser = user(10L, "user@test.com", UserRole.USER);
            Notification notification = notification(1L, currentUser, NotificationType.VISIT,
                    LocalDateTime.of(2026, 3, 28, 9, 0));

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
            when(notificationRepository.findByIdAndUser_Id(1L, 10L)).thenReturn(Optional.of(notification));

            NotificationResponse response = notificationService.markAsRead(1L, "user@test.com");

            assertThat(response.read()).isTrue();
            verify(notificationRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Marca todas las notificaciones no leídas del usuario")
        void markAllAsRead_returnsUpdatedCount() {
            User currentUser = user(10L, "user@test.com", UserRole.USER);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
            when(notificationRepository.markAllAsRead(eq(10L), any(LocalDateTime.class))).thenReturn(4);

            long updated = notificationService.markAllAsRead("user@test.com");

            assertThat(updated).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Cuenta solo las notificaciones no leídas")
        void getUnreadCount_returnsCount() {
            User currentUser = user(10L, "user@test.com", UserRole.USER);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
            when(notificationRepository.countByUser_IdAndReadAtIsNull(10L)).thenReturn(3L);

            long unreadCount = notificationService.getUnreadCount("user@test.com");

            assertThat(unreadCount).isEqualTo(3L);
        }
    }

    @Nested
    @DisplayName("delete()")
    class DeleteTests {

        @Test
        @DisplayName("Elimina la notificación propia")
        void deleteOwnedNotification_callsRepositoryDelete() {
            User currentUser = user(10L, "user@test.com", UserRole.USER);
            Notification notification = notification(1L, currentUser, NotificationType.ALERT, null);

            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
            when(notificationRepository.findByIdAndUser_Id(1L, 10L)).thenReturn(Optional.of(notification));

            notificationService.delete(1L, "user@test.com");

            verify(notificationRepository).delete(notification);
        }

        @Test
        @DisplayName("No permite operar sobre una notificación inexistente")
        void deleteMissingNotification_throwsNotFound() {
            User currentUser = user(10L, "user@test.com", UserRole.USER);
            when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
            when(notificationRepository.findByIdAndUser_Id(999L, 10L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.delete(999L, "user@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Test
    @DisplayName("create() persiste el usuario destino correcto")
    void create_persistsResolvedTargetUser() {
        User currentUser = user(10L, "user@test.com", UserRole.USER);
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(currentUser));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId(77L);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());
            return notification;
        });

        CreateNotificationRequest request = new CreateNotificationRequest(
                null,
                NotificationType.SYSTEM,
                "Bienvenido",
                "Tu cuenta está lista",
                null,
                ""
        );

        notificationService.create(request, "user@test.com");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getUser().getId()).isEqualTo(10L);
        assertThat(captor.getValue().getActionUrl()).isNull();
    }

    private User user(Long id, String email, UserRole role) {
        User user = User.builder()
                .email(email)
                .passwordHash("hash")
                .name("Test User")
                .role(role)
                .build();
        user.setId(id);
        return user;
    }

    private Notification notification(Long id, User user, NotificationType type, LocalDateTime readAt) {
        Notification notification = Notification.builder()
                .user(user)
                .type(type)
                .title("Título " + id)
                .message("Mensaje " + id)
                .data(Map.of("notificationId", id))
                .actionUrl("/notifications/" + id)
                .readAt(readAt)
                .build();
        notification.setId(id);
        notification.setCreatedAt(LocalDateTime.of(2026, 3, 29, 10, 0));
        notification.setUpdatedAt(LocalDateTime.of(2026, 3, 29, 10, 0));
        return notification;
    }
}
