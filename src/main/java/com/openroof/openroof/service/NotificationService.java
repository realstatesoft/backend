package com.openroof.openroof.service;

import com.openroof.openroof.dto.notification.CreateNotificationRequest;
import com.openroof.openroof.dto.notification.NotificationResponse;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.notification.Notification;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.NotificationRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationResponse create(CreateNotificationRequest request, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        User targetUser = resolveTargetUser(currentUser, request.userId());

        Notification notification = Notification.builder()
                .user(targetUser)
                .type(request.type())
                .title(request.title().trim())
                .message(request.message().trim())
                .data(request.data())
                .actionUrl(normalizeOptionalText(request.actionUrl()))
                .build();

        return toResponse(notificationRepository.save(notification));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        return notificationRepository.findByUser_IdOrderByCreatedAtDesc(currentUser.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse getByIdForCurrentUser(Long notificationId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        return toResponse(getOwnedNotification(notificationId, currentUser.getId()));
    }

    public NotificationResponse markAsRead(Long notificationId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        Notification notification = getOwnedNotification(notificationId, currentUser.getId());

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notification = notificationRepository.save(notification);
        }

        return toResponse(notification);
    }

    public long markAllAsRead(String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        return notificationRepository.markAllAsRead(currentUser.getId(), LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        return notificationRepository.countByUser_IdAndReadAtIsNull(currentUser.getId());
    }

    public void delete(Long notificationId, String currentUserEmail) {
        User currentUser = getUserByEmail(currentUserEmail);
        Notification notification = getOwnedNotification(notificationId, currentUser.getId());
        notificationRepository.delete(notification);
    }

    private User resolveTargetUser(User currentUser, Long requestedUserId) {
        if (requestedUserId == null) {
            return currentUser;
        }

        if (currentUser.getRole() != UserRole.ADMIN && !requestedUserId.equals(currentUser.getId())) {
            throw new ForbiddenException("Solo un ADMIN puede crear notificaciones para otros usuarios");
        }

        return userRepository.findById(requestedUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con ID: " + requestedUserId));
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado: " + email));
    }

    private Notification getOwnedNotification(Long notificationId, Long userId) {
        return notificationRepository.findByIdAndUser_Id(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Notificación no encontrada con ID: " + notificationId));
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUser().getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getData(),
                notification.getActionUrl(),
                notification.getReadAt(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                notification.getUpdatedAt()
        );
    }
}
