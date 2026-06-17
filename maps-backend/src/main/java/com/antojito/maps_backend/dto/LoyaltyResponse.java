package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del perfil de fidelizacion")
public class LoyaltyResponse {

    @Schema(description = "UUID del cliente", example = "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9")
    private UUID clientId;

    @Schema(description = "Puntos acumulados", example = "120")
    private Integer points;

    @Schema(description = "Nivel de fidelizacion", example = "PLATA")
    private String level;
}
