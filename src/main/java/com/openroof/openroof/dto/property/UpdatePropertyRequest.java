package com.openroof.openroof.dto.property;

import com.openroof.openroof.model.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para actualización parcial de una propiedad.
 * Todos los campos son opcionales (nullable); solo se actualizan los que llegan
 * con valor.
 */
public record UpdatePropertyRequest(

        @Size(max = 255, message = "El título no puede exceder 255 caracteres") String title,

        String description,

        PropertyType propertyType,

        PropertyCategory category,

        ListingType listingType,

        @DecimalMin(value = "0.0", inclusive = false, message = "El alquiler debe ser mayor a 0") BigDecimal rentAmount,

        @Size(max = 3, message = "La moneda debe tener máximo 3 caracteres") String rentCurrency,
        @Size(max = 20) String rentFrequency,
        @Size(max = 20) String rentBillingCycle,

        @Size(max = 500, message = "La dirección no puede exceder 500 caracteres") String address,

        BigDecimal lat,
        BigDecimal lng,
        Long locationId,

        @Positive(message = "El precio debe ser mayor a 0") BigDecimal price,

        @Min(value = 0, message = "Los dormitorios no pueden ser negativos") Integer bedrooms,

        @DecimalMin(value = "0", message = "Los baños no pueden ser negativos") BigDecimal bathrooms,

        @Min(value = 0, message = "Los medios baños no pueden ser negativos") Integer halfBathrooms,

        @Min(value = 0, message = "Los baños completos no pueden ser negativos") Integer fullBathrooms,

        @DecimalMin(value = "0", message = "La superficie no puede ser negativa") BigDecimal surfaceArea,

        @DecimalMin(value = "0", message = "La superficie construida no puede ser negativa") BigDecimal builtArea,

        @Min(value = 0, message = "Los estacionamientos no pueden ser negativos") Integer parkingSpaces,

        @Min(value = 1, message = "Los pisos deben ser al menos 1") Integer floorsCount,

        Long agentId,

        Integer constructionYear,
        ConstructionStatus constructionStatus,
        @Size(max = 100) String structureMaterial,
        @Size(max = 100) String wallsMaterial,
        @Size(max = 100) String floorMaterial,
        @Size(max = 100) String roofMaterial,

        @Size(max = 50) String waterConnection,
        @Size(max = 50) String sanitaryInstallation,
        @Size(max = 50) String electricityInstallation,

        Availability availability,
        Visibility visibility,

        @Valid List<PropertyRoomDto> rooms,

        @Valid List<PropertyMediaDto> media,

        List<Long> exteriorFeatureIds) {

    @AssertTrue(message = "rentCurrency es obligatorio cuando listingType es RENT y rentAmount está presente")
    public boolean isRentCurrencyValid() {
        if (listingType == ListingType.RENT && rentAmount != null) {
            return rentCurrency != null && !rentCurrency.isBlank();
        }
        return true;
    }
}
