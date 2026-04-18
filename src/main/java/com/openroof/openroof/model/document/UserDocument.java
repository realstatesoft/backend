package com.openroof.openroof.model.document;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.DocumentStatus;
import com.openroof.openroof.model.enums.DocumentType;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

/**
 * Entidad que representa un documento personal de un usuario.
 * Almacena metadatos del archivo; el contenido real vive en Supabase Storage.
 */
@Entity
@Table(name = "user_documents", indexes = {
        @Index(name = "idx_user_documents_user", columnList = "user_id"),
        @Index(name = "idx_user_documents_status", columnList = "document_status")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDocument extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 50)
    @Builder.Default
    private DocumentStatus documentStatus = DocumentStatus.PENDING;

    /** URL pública en Supabase Storage */
    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    /** Nombre lógico del archivo dentro del bucket (key) */
    @Column(name = "filename", nullable = false, length = 512)
    private String filename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size")
    private Long size;

    /** Notas del administrador al aprobar/rechazar */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
