package com.openroof.openroof.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta después de subir una imagen.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageUploadResponse {

    private Long id;
    private String url;
    private String filename;
    private long size;
    private String contentType;
    private String uploadedBy;
    private LocalDateTime createdAt;
}
