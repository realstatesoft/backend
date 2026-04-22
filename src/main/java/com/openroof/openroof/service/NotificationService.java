package com.openroof.openroof.service;

import com.openroof.openroof.dto.notification.CreateNotificationRequest;
import com.openroof.openroof.dto.notification.NotificationResponse;
import com.openroof.openroof.exception.BadRequestException;
import com.openroof.openroof.exception.ForbiddenException;
import com.openroof.openroof.exception.ResourceNotFoundException;
import com.openroof.openroof.model.enums.NotificationType;
import com.openroof.openroof.model.enums.UserRole;
import com.openroof.openroof.model.notification.Notification;
import com.openroof.openroof.model.user.User;
import com.openroof.openroof.repository.NotificationRepository;
import com.openroof.openroof.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.openroof.openroof.model.property.Property;

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
    public Page<NotificationResponse> getMyNotifications(String currentUserEmail, String filter, Pageable pageable) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long userId = currentUser.getId();

        if ("UNREAD".equalsIgnoreCase(filter)) {
            return notificationRepository.findByUser_IdAndReadAtIsNullOrderByCreatedAtDesc(userId, pageable)
                    .map(this::toResponse);
        } else if ("READ".equalsIgnoreCase(filter)) {
            return notificationRepository.findByUser_IdAndReadAtIsNotNullOrderByCreatedAtDesc(userId, pageable)
                    .map(this::toResponse);
        } else if ("PROPERTY".equalsIgnoreCase(filter)) {
            return notificationRepository.findByUser_IdAndTypeOrderByCreatedAtDesc(userId, NotificationType.PROPERTY, pageable)
                    .map(this::toResponse);
        } else {
            return notificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable)
                    .map(this::toResponse);
        }
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

    public long deleteAll(String currentUserEmail, String filter) {
        User currentUser = getUserByEmail(currentUserEmail);
        Long userId = currentUser.getId();
        LocalDateTime now = LocalDateTime.now();

        if (filter == null || filter.trim().isEmpty() || "ALL".equalsIgnoreCase(filter)) {
            return notificationRepository.deleteAllByUser(userId, now);
        } else if ("UNREAD".equalsIgnoreCase(filter)) {
            return notificationRepository.deleteAllUnreadByUser(userId, now);
        } else if ("READ".equalsIgnoreCase(filter)) {
            return notificationRepository.deleteAllReadByUser(userId, now);
        } else if ("PROPERTY".equalsIgnoreCase(filter)) {
            return notificationRepository.deleteAllByTypeByUser(userId, NotificationType.PROPERTY, now);
        } else {
            throw new BadRequestException("Filtro no válido para eliminación: " + filter);
        }
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void createPropertyPendingNotification(Property property) {
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);

        for (User admin : admins) {
            Notification notification = Notification.builder()
                    .user(admin)
                    .type(com.openroof.openroof.model.enums.NotificationType.PROPERTY)
                    .title("Nueva propiedad pendiente")
                    .message(String.format("La propiedad '%s' (ID: %d) ha sido creada y está pendiente de revisión.",
                            property.getTitle(), property.getId()))
                    .data(Map.of("propertyId", property.getId()))
                    .actionUrl("/admin/properties/" + property.getId())
                    .build();
            notificationRepository.save(notification);
        }
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
