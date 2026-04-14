package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Respuesta de login admin")
public record AdminLoginResponse(
        @Schema(description = "UUID del admin autenticado", example = "f792617d-0d5d-4881-b5f6-679bcf2c37f8")
        UUID adminId,
        @Schema(description = "Mail del admin autenticado", example = "admin@antojitosmaps.com")
        String mail,
        @Schema(description = "Mensaje de estado", example = "login correcto")
        String message) {
}
