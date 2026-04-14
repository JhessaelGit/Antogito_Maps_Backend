package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Respuesta de administrador")
public record AdminResponse(
        @Schema(description = "UUID del admin", example = "f792617d-0d5d-4881-b5f6-679bcf2c37f8")
        UUID uuid,
        @Schema(description = "Mail del admin", example = "admin@antojitosmaps.com")
        String mail,
        @Schema(description = "Indica si el admin esta eliminado logicamente", example = "false")
        Boolean isDeleted,
        @Schema(description = "Fecha de eliminacion logica, null si sigue activo")
        LocalDateTime deletedAt) {
}
