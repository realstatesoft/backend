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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.openroof.openroof.model.property.Property;
import com.openroof.openroof.model.rental.Lease;
import com.openroof.openroof.model.rental.RentalApplication;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

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
        } else if ("RESERVATION".equalsIgnoreCase(filter)) {
            return notificationRepository.findByUser_IdAndTypeOrderByCreatedAtDesc(userId, NotificationType.RESERVATION, pageable)
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
        } else if ("RESERVATION".equalsIgnoreCase(filter)) {
            return notificationRepository.deleteAllByTypeByUser(userId, NotificationType.RESERVATION, now);
        } else {
            throw new BadRequestException("Filtro no válido para eliminación: " + filter);
        }
    }

    // ─── Rental application notifications (OR-237) ────────────────────────────

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyApplicationSubmitted(RentalApplication application) {
        Property property = application.getProperty();
        User landlord = property != null ? property.getOwner() : null;
        User applicant = application.getApplicant();
        if (landlord == null || applicant == null) {
            return;
        }

        Notification notif = Notification.builder()
                .user(landlord)
                .type(NotificationType.SYSTEM)
                .title("Nueva solicitud de alquiler")
                .message(String.format("%s envió una solicitud para '%s'.",
                        applicant.getName(), property.getTitle()))
                .data(Map.of(
                        "applicationId", application.getId(),
                        "propertyId", property.getId()))
                .actionUrl("/rental-applications/" + application.getId())
                .build();
        notificationRepository.save(notif);

        emailService.sendApplicationSubmittedEmail(
                landlord.getEmail(),
                landlord.getName(),
                applicant.getName(),
                property.getTitle(),
                application.getMonthlyIncome(),
                application.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyApplicationApproved(RentalApplication application) {
        User applicant = application.getApplicant();
        Property property = application.getProperty();
        if (applicant == null || property == null) {
            return;
        }

        Notification notif = Notification.builder()
                .user(applicant)
                .type(NotificationType.SYSTEM)
                .title("Aplicación aprobada")
                .message(String.format("Tu solicitud para '%s' fue aprobada.", property.getTitle()))
                .data(Map.of(
                        "applicationId", application.getId(),
                        "propertyId", property.getId()))
                .actionUrl("/rental-applications/" + application.getId())
                .build();
        notificationRepository.save(notif);

        emailService.sendApplicationApprovedEmail(
                applicant.getEmail(),
                applicant.getName(),
                property.getTitle(),
                null,
                application.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyApplicationRejected(RentalApplication application, String publicReason) {
        User applicant = application.getApplicant();
        Property property = application.getProperty();
        if (applicant == null || property == null) {
            return;
        }

        Notification notif = Notification.builder()
                .user(applicant)
                .type(NotificationType.SYSTEM)
                .title("Aplicación rechazada")
                .message(String.format("Tu solicitud para '%s' fue rechazada.", property.getTitle()))
                .data(Map.of(
                        "applicationId", application.getId(),
                        "propertyId", property.getId()))
                .actionUrl("/rental-applications/" + application.getId())
                .build();
        notificationRepository.save(notif);

        emailService.sendApplicationRejectedEmail(
                applicant.getEmail(),
                applicant.getName(),
                property.getTitle(),
                publicReason,
                application.getId());
    }

    // ─── Lease notifications (OR-238) ─────────────────────────────────────────

    public enum SignerSide { LANDLORD, TENANT }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyLeaseSentForSignature(Lease lease) {
        Property property = lease.getProperty();
        User landlord = lease.getLandlord();
        User tenant = lease.getPrimaryTenant();
        if (property == null || landlord == null || tenant == null) {
            return;
        }
        String title = property.getTitle();
        LocalDateTime expiresAt = lease.getSignatureTokenExpiresAt();

        if (landlord.getEmail() != null) {
            notificationRepository.save(buildLeaseNotif(landlord, lease,
                    "Contrato listo para firmar",
                    String.format("El contrato para '%s' está listo para tu firma.", title)));
            emailService.sendLeaseSentForSignatureEmail(
                    landlord.getEmail(), landlord.getName(), title,
                    signatureLink(lease.getId(), lease.getSignatureTokenLandlord()), expiresAt);
        }
        if (tenant.getEmail() != null) {
            notificationRepository.save(buildLeaseNotif(tenant, lease,
                    "Contrato listo para firmar",
                    String.format("El contrato para '%s' está listo para tu firma.", title)));
            emailService.sendLeaseSentForSignatureEmail(
                    tenant.getEmail(), tenant.getName(), title,
                    signatureLink(lease.getId(), lease.getSignatureTokenTenant()), expiresAt);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyLeaseSigned(Lease lease, SignerSide signerSide) {
        Property property = lease.getProperty();
        User landlord = lease.getLandlord();
        User tenant = lease.getPrimaryTenant();
        if (property == null || landlord == null || tenant == null) {
            return;
        }
        boolean signedByLandlord = signerSide == SignerSide.LANDLORD;
        User signer = signedByLandlord ? landlord : tenant;
        User recipient = signedByLandlord ? tenant : landlord;
        String title = property.getTitle();
        String pendingMessage = lease.isSigned()
                ? "Ambas partes ya firmaron. El contrato podrá activarse."
                : "Falta tu firma para activar el contrato.";

        notificationRepository.save(buildLeaseNotif(recipient, lease,
                "Contrato firmado",
                String.format("%s firmó el contrato para '%s'.", signer.getName(), title)));

        emailService.sendLeaseSignedEmail(
                recipient.getEmail(), recipient.getName(), signer.getName(),
                title, pendingMessage, lease.getId());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyLeaseActivated(Lease lease, java.time.LocalDate firstInstallmentDueDate) {
        Property property = lease.getProperty();
        User landlord = lease.getLandlord();
        User tenant = lease.getPrimaryTenant();
        if (property == null || landlord == null || tenant == null) {
            return;
        }
        String title = property.getTitle();
        EmailService.LeaseSummary summary = new EmailService.LeaseSummary(
                lease.getMonthlyRent(), lease.getStartDate(), lease.getEndDate(),
                firstInstallmentDueDate);

        notificationRepository.save(buildLeaseNotif(tenant, lease,
                "Contrato activo",
                String.format("Tu contrato para '%s' está activo.", title)));
        emailService.sendLeaseActivatedTenantEmail(
                tenant.getEmail(), tenant.getName(), title, summary, lease.getId());

        notificationRepository.save(buildLeaseNotif(landlord, lease,
                "Contrato activo",
                String.format("El contrato para '%s' fue activado.", title)));
        emailService.sendLeaseActivatedLandlordEmail(
                landlord.getEmail(), landlord.getName(), tenant.getName(),
                title, summary, lease.getId());
    }

    private Notification buildLeaseNotif(User user, Lease lease, String title, String message) {
        return Notification.builder()
                .user(user)
                .type(NotificationType.SYSTEM)
                .title(title)
                .message(message)
                .data(Map.of(
                        "leaseId", lease.getId(),
                        "propertyId", lease.getProperty().getId()))
                .actionUrl("/leases/" + lease.getId())
                .build();
    }

    private String signatureLink(Long leaseId, java.util.UUID token) {
        if (token == null) {
            return "/leases/" + leaseId;
        }
        return "/leases/" + leaseId + "/sign?token=" + token;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
