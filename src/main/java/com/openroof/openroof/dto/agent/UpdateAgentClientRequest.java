package com.openroof.openroof.dto.agent;

import com.openroof.openroof.model.enums.ClientStatus;
import com.openroof.openroof.model.enums.ContactMethod;
import com.openroof.openroof.model.enums.MaritalStatus;
import com.openroof.openroof.model.enums.Priority;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PastOrPresent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateAgentClientRequest(

        ClientStatus status,

        Priority priority,

        List<String> tags,

        // Budget range
        @DecimalMin(value = "0.0", message = "El presupuesto mínimo no puede ser negativo")
        BigDecimal minBudget,
        @DecimalMin(value = "0.0", message = "El presupuesto máximo no puede ser negativo")
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

    @AssertTrue(message = "Los rangos de presupuesto, habitaciones o baños son inconsistentes (min > max)")
    private boolean isRangesValid() {
        boolean budgetValid = minBudget == null || maxBudget == null || minBudget.compareTo(maxBudget) <= 0;
        boolean bedroomsValid = minBedrooms == null || maxBedrooms == null || minBedrooms <= maxBedrooms;
        boolean bathroomsValid = minBathrooms == null || maxBathrooms == null || minBathrooms <= maxBathrooms;
        return budgetValid && bedroomsValid && bathroomsValid;
    }
}
