package com.openroof.openroof.dto.agent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateAgentProfileRequest(

        @Size(max = 255, message = "El nombre de empresa no puede exceder 255 caracteres")
        String companyName,

        String bio,

        @Min(value = 0, message = "Los años de experiencia no pueden ser negativos")
        Integer experienceYears,

        @Size(max = 100, message = "El número de licencia no puede exceder 100 caracteres")
        String licenseNumber,

        List<Long> specialtyIds,

        @Valid
        List<AgentSocialMediaDto> socialMedia
) {
}
