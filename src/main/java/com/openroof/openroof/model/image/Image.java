package com.openroof.openroof.model.image;

import com.openroof.openroof.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Entidad que persiste los metadatos de una imagen subida a Supabase Storage.
 */
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "images")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Image extends BaseEntity {

    @Column(name = "url", nullable = false, length = 1024)
    private String url;

    @Column(name = "filename", nullable = false, length = 512)
    private String filename;

    @Column(name = "content_type", length = 100)
    private String contentType;

    @Column(name = "size")
    private Long size;

    @Column(name = "uploaded_by", length = 255)
    private String uploadedBy;
}
