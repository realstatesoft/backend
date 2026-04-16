package com.openroof.openroof.dto.document;

import com.openroof.openroof.model.document.UserDocument;
import com.openroof.openroof.model.enums.DocumentStatus;
import com.openroof.openroof.model.enums.DocumentType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para un documento personal del usuario.
 */
@Getter
@Builder
public class UserDocumentResponse {

    private Long id;

    // ─── Datos del usuario ──────────────────────────────────────────────────────
    private Long userId;
    private String userName;
    private String userEmail;
    private String userPhone;
    private String userRole;
    private String userAvatarUrl;
    private LocalDateTime userCreatedAt;

    // ─── Datos del documento ────────────────────────────────────────────────────
    private DocumentType documentType;
    private DocumentStatus documentStatus;
    private String url;
    private String filename;
    private String contentType;
    private Long size;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserDocumentResponse from(UserDocument doc) {
        var user = doc.getUser();
        return UserDocumentResponse.builder()
                .id(doc.getId())
                .userId(user != null ? user.getId() : null)
                .userName(user != null ? user.getName() : null)
                .userEmail(user != null ? user.getEmail() : null)
                .userPhone(user != null ? user.getPhone() : null)
                .userRole(user != null ? user.getRole().name() : null)
                .userAvatarUrl(user != null ? user.getAvatarUrl() : null)
                .userCreatedAt(user != null ? user.getCreatedAt() : null)
                .documentType(doc.getDocumentType())
                .documentStatus(doc.getDocumentStatus())
                .url(doc.getUrl())
                .filename(doc.getFilename())
                .contentType(doc.getContentType())
                .size(doc.getSize())
                .notes(doc.getNotes())
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
