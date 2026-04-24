package com.openroof.openroof.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para editar los datos personales del usuario autenticado.
 * Todos los campos son opcionales: solo se actualizan los que vengan en el request.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String name;

    @Pattern(regexp = "^\\+?[0-9\\s\\-().]{7,20}$", message = "Formato de teléfono inválido")
    private String phone;

    @Size(max = 2048, message = "La URL del avatar es demasiado larga")
    private String avatarUrl;
}
