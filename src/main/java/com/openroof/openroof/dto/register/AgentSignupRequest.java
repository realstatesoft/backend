package com.openroof.openroof.dto.register;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.openroof.openroof.common.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO específico para el registro de agentes.
 * Este registro crea una solicitud de agente pendiente de aprobación administrativa.
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
    @Pattern(regexp = PasswordPolicy.COMPLEXITY_REGEX, message = PasswordPolicy.COMPLEXITY_MESSAGE)
    private String password;

    @NotBlank(message = "El teléfono es obligatorio para agentes")
    private String phone;

    // Campos específicos de agente (opcionales en el registro inicial)
    private String companyName;
    private String licenseNumber;
    private Integer experienceYears;

    /**
     * Convierte este DTO a RegisterRequest solicitando rol AGENT.
     * El backend lo transforma a AGENT_PENDING hasta aprobación.
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