package com.openroof.openroof.dto.lead;

import com.openroof.openroof.model.enums.PropertyCategory;
import com.openroof.openroof.model.enums.PropertyType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Map;

/**
 * DTO para crear un Lead desde el Sell Wizard del frontend.
 * Contiene toda la información recopilada en el wizard de venta/alquiler.
 */
public record CreateLeadFromWizardRequest(
        // Agente seleccionado
        @NotNull(message = "Debe seleccionar un agente")
        Long agentId,

        // Información de contacto del propietario
        @NotBlank(message = "El nombre es requerido")
        @Size(max = 100)
        String firstName,

        @NotBlank(message = "El apellido es requerido")
        @Size(max = 100)
        String lastName,

        @NotBlank(message = "El teléfono es requerido")
        @Size(max = 20)
        String phone,

        @Email(message = "Email inválido")
        @Size(max = 255)
        String email,

        // Información de la propiedad
        @NotBlank(message = "La dirección es requerida")
        String address,
        
        Double latitude,
        Double longitude,

        @NotNull(message = "El tipo de propiedad es requerido")
        PropertyType propertyType,

        @NotNull(message = "La categoría es requerida")
        PropertyCategory category,

        // Detalles de la propiedad
        String surfaceArea,
        String builtArea,
        String yearBuilt,
        Integer bedrooms,
        Integer halfBath,
        Integer threeQuarterBath,
        Integer floors,

        // Features
        Boolean hasPool,
        Integer parkingSpaces,
        Boolean hasSecureEntry,
        Boolean hasBasement,
        String basementArea,

        // Conditions
        String exteriorCondition,
        String livingRoomCondition,
        String bathroomCondition,
        String kitchenCondition,
        String countertopType,

        // HOA & Special
        Boolean hasHOA,
        List<String> specialConditions,

        // Relación y timeline
        String agentRelationship,
        String timeline,

        // Datos adicionales como mapa
        Map<String, Object> additionalData
) {
    public String getFullName() {
        return (firstName + " " + lastName).trim();
    }
}
