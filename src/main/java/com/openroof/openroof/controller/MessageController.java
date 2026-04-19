package com.openroof.openroof.controller;

import com.openroof.openroof.common.ApiResponse;
import com.openroof.openroof.dto.message.ConversationResponse;
import com.openroof.openroof.dto.message.MessageResponse;
import com.openroof.openroof.dto.message.SendMessageRequest;
import com.openroof.openroof.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "Messages", description = "Mensajería entre usuarios")
public class MessageController {

    private final MessageService messageService;

    @GetMapping("/conversations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Listar conversaciones del usuario autenticado")
    public ResponseEntity<ApiResponse<List<ConversationResponse>>> getConversations(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageService.getConversations(auth.getName())));
    }

    @GetMapping("/conversations/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener cantidad de mensajes sin leer")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageService.getUnreadCount(auth.getName())));
    }

    @GetMapping("/conversations/{peerId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Obtener mensajes de una conversación")
    public ResponseEntity<ApiResponse<List<MessageResponse>>> getMessages(
            Authentication auth,
            @PathVariable Long peerId) {
        return ResponseEntity.ok(ApiResponse.ok(
                messageService.getMessages(auth.getName(), peerId)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Enviar un mensaje")
    public ResponseEntity<ApiResponse<MessageResponse>> send(
            Authentication auth,
            @Valid @RequestBody SendMessageRequest request) {
        MessageResponse response = messageService.send(auth.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Mensaje enviado exitosamente"));
    }

    @PutMapping("/conversations/{peerId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Marcar conversación como leída")
    public ResponseEntity<Void> markAsRead(
            Authentication auth,
            @PathVariable Long peerId) {
        messageService.markAsRead(auth.getName(), peerId);
        return ResponseEntity.noContent().build();
    }
}
