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
@Schema(description = "Payload para actualizar perfil de administrador")
public class AdminUpdateRequest {

    @NotBlank(message = "El mail es obligatorio")
    @Email(message = "El mail no tiene formato valido")
    @Size(max = 150, message = "El mail no puede exceder 150 caracteres")
    @Schema(description = "Nuevo mail del admin", example = "admin.editado@antojitosmaps.com")
    private String mail;

    @NotBlank(message = "La password es obligatoria")
    @Size(max = 255, message = "La password no puede exceder 255 caracteres")
    @Schema(description = "Nueva password del admin", example = "AdminEditado2026!")
    private String password;
}
