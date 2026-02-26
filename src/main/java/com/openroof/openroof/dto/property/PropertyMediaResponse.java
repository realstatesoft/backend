package com.openroof.openroof.dto.property;

import com.openroof.openroof.model.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para imágenes/media de una propiedad.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyMediaResponse {

    private Long id;
    private Long propertyId;
    private String url;
    private String thumbnailUrl;
    private MediaType type;
    private Boolean isPrimary;
    private Integer orderIndex;
    private String title;
    private LocalDateTime createdAt;
}
