package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload de login")
public class LoginRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene formato valido")
    @Size(max = 150, message = "El email no puede exceder 150 caracteres")
    @Schema(description = "Correo del usuario que inicia sesion", example = "usuario@antojitos.com")
    private String email;
}
