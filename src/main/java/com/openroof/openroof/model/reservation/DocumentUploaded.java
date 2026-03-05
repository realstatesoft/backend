package com.openroof.openroof.model.reservation;

import com.openroof.openroof.common.BaseEntity;
import com.openroof.openroof.model.enums.DocumentStatus;
import com.openroof.openroof.model.enums.DocumentType;
import com.openroof.openroof.model.user.User;
import jakarta.persistence.*;
import lombok.*;

import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "documents_uploaded", indexes = {
        @Index(name = "idx_documents_reservation", columnList = "reservation_id"),
        @Index(name = "idx_documents_uploaded_by", columnList = "uploaded_by_id"),
        @Index(name = "idx_documents_type", columnList = "document_type"),
        @Index(name = "idx_documents_status", columnList = "status")
})
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentUploaded extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_id", nullable = false)
    private User uploadedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 50)
    private DocumentType documentType;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private DocumentStatus status = DocumentStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;
}
