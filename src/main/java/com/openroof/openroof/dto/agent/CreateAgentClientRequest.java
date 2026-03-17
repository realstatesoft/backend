package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ContactMethod;
import com.openroof.openroof.model.enums.MaritalStatus;
import com.openroof.openroof.model.enums.Priority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateAgentClientRequest(

        @NotNull(message = "El ID del agente es obligatorio") Long agentId,

        @NotNull(message = "El ID del usuario/cliente es obligatorio") Long userId,

        ClientStatus status,

        Priority priority,

        List<String> tags,

        // Budget range
        @PositiveOrZero(message = "El presupuesto mínimo no puede ser negativo")
        BigDecimal minBudget,
        @PositiveOrZero(message = "El presupuesto máximo no puede ser negativo")
        BigDecimal maxBudget,

        // Bedroom range
        @Min(value = 0, message = "El número mínimo de habitaciones no puede ser negativo")
        Integer minBedrooms,
        @Min(value = 0, message = "El número máximo de habitaciones no puede ser negativo")
        Integer maxBedrooms,

        // Bathroom range
        @Min(value = 0, message = "El número mínimo de baños no puede ser negativo")
        Integer minBathrooms,
        @Min(value = 0, message = "El número máximo de baños no puede ser negativo")
        Integer maxBathrooms,

        ContactMethod preferredContactMethod,

        // Detalle personal
        @PastOrPresent(message = "La fecha de nacimiento no puede ser una fecha futura")
        LocalDate birthDate,
        MaritalStatus maritalStatus,
        String occupation,
        @DecimalMin(value = "0.00", message = "El ingreso anual no puede ser negativo")
        @Digits(integer = 12, fraction = 2, message = "El ingreso anual debe tener máximo 2 decimales")
        BigDecimal annualIncome,
        String address,
        String sourceChannel,

        // Preferencias
        List<String> preferredPropertyTypes,
        List<String> preferredAreas,
        List<String> desiredFeatures,

        String notes) {

    public CreateAgentClientRequest {
        if (birthDate != null && birthDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser una fecha futura");
        }
        if (annualIncome != null) {
            if (annualIncome.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("El ingreso anual no puede ser negativo");
            }
            if (annualIncome.scale() > 2) {
                throw new IllegalArgumentException("El ingreso anual no puede tener más de 2 decimales");
            }
        }
    }

    @AssertTrue(message = "El presupuesto mínimo no puede ser mayor que el máximo")
    private boolean isBudgetRangeValid() {
        if (minBudget == null || maxBudget == null) return true;
        return minBudget.compareTo(maxBudget) <= 0;
    }

    @AssertTrue(message = "El mínimo de habitaciones no puede ser mayor que el máximo")
    private boolean isBedroomRangeValid() {
        if (minBedrooms == null || maxBedrooms == null) return true;
        return minBedrooms <= maxBedrooms;
    }

    @AssertTrue(message = "El mínimo de baños no puede ser mayor que el máximo")
    private boolean isBathroomRangeValid() {
        if (minBathrooms == null || maxBathrooms == null) return true;
        return minBathrooms <= maxBathrooms;
    }
}
