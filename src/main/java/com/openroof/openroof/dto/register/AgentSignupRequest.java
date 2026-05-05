package com.openroof.openroof.dto.register;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO específico para el registro de agentes.
 * Hereda la misma estructura que RegisterRequest pero está diseñado específicamente 
 * para simplificar el registro de agentes desde el frontend, forzando automáticamente el Role.AGENT.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AgentSignupRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El teléfono es obligatorio para agentes")
    private String phone;

    // Campos específicos de agente (opcionales en el registro inicial)
    private String companyName;
    private String licenseNumber;
    private Integer experienceYears;

    /**
     * Convierte este DTO a RegisterRequest estableciendo automáticamente el role como AGENT.
     */
    public RegisterRequest toRegisterRequest() {
        return RegisterRequest.builder()
                .name(this.name)
                .email(this.email)
                .password(this.password)
                .phone(this.phone)
                .role("AGENT") // Forzar role AGENT
                .build();
    }
}