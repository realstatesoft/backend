package com.openroof.openroof.controller;

import java.security.Principal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.notification.CreateNotificationRequest;
import com.openroof.openroof.dto.notification.NotificationResponse;
import com.openroof.openroof.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "CRUD de notificaciones del usuario autenticado")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Crear una notificación")
    public ResponseEntity<ApiResponse<NotificationResponse>> create(
            @Valid @RequestBody CreateNotificationRequest request,
            Principal principal) {

        NotificationResponse response = notificationService.create(request, principal.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Notificación creada exitosamente"));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar mis notificaciones")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getMyNotifications(
            @Parameter(description = "Filtro de notificaciones: READ, UNREAD o PROPERTY")
            @RequestParam(required = false) String filter,
            Pageable pageable,
            Principal principal) {
        Page<NotificationResponse> response = notificationService.getMyNotifications(principal.getName(), filter, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/me/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Contar mis notificaciones no leídas")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Principal principal) {
        long unreadCount = notificationService.getUnreadCount(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(unreadCount));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener una notificación propia por ID")
    public ResponseEntity<ApiResponse<NotificationResponse>> getById(
            @Parameter(description = "ID de la notificación") @PathVariable Long id,
            Principal principal) {

        NotificationResponse response = notificationService.getByIdForCurrentUser(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar una notificación propia como leída")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @Parameter(description = "ID de la notificación") @PathVariable Long id,
            Principal principal) {

        NotificationResponse response = notificationService.markAsRead(id, principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Notificación marcada como leída"));
    }

    @PutMapping("/me/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar todas mis notificaciones como leídas")
    public ResponseEntity<ApiResponse<Long>> markAllAsRead(Principal principal) {
        long updatedCount = notificationService.markAllAsRead(principal.getName());
        return ResponseEntity.ok(ApiResponse.ok(updatedCount, "Notificaciones marcadas como leídas"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Eliminar una notificación propia")
    public ResponseEntity<Void> delete(
            @Parameter(description = "ID de la notificación") @PathVariable Long id,
            Principal principal) {

        notificationService.delete(id, principal.getName());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Eliminar todas mis notificaciones (opcionalmente filtradas)")
    public ResponseEntity<ApiResponse<Long>> deleteAll(
            @Parameter(description = "Filtro de notificaciones a eliminar: READ, UNREAD o PROPERTY")
            @RequestParam(required = false) String filter,
            Principal principal) {

        long deletedCount = notificationService.deleteAll(principal.getName(), filter);
        return ResponseEntity.ok(ApiResponse.ok(deletedCount, "Notificaciones eliminadas exitosamente"));
    }
}
