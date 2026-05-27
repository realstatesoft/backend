package com.openroof.openroof.dto.register;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Auth: Enrique Rios
 * Desc: DTO para la captura de datos en el registro de nuevos usuarios.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "El nombre es obligatorio")
    private String name;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).{10,}$",
        message = "La contraseña debe tener al menos 10 caracteres e incluir mayúsculas, minúsculas, números y un carácter especial"
    )
    private String password;

    private String phone;

    @NotBlank(message = "El rol es obligatorio")
    private String role;
}