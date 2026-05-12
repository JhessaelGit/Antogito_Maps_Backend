package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Credenciales del cliente para iniciar sesion")
public class ClientLoginRequest {

    @Email(message = "El correo debe ser valido")
    @NotBlank(message = "El correo es requerido")
    @Schema(description = "Correo electronico del cliente", example = "cliente@gmail.com")
    private String email;

    @NotBlank(message = "La contrasena es requerida")
    @Schema(description = "Contrasena del cliente", example = "cliente123")
    private String password;
}
