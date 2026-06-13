package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Data;

@Data
public class AddPointsRequest {

    @NotNull(message = "El UUID del cliente es requerido")
    @Schema(description = "UUID del cliente", example = "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9")
    private UUID clientId;

    @NotNull(message = "La cantidad de puntos es requerida")
    @Min(value = 1, message = "Los puntos deben ser mayores a 0")
    @Schema(description = "Puntos a agregar", example = "50")
    private Integer points;

    @NotBlank(message = "La razon es requerida")
    @Schema(description = "Motivo de la asignacion de puntos", example = "Compra de producto")
    private String reason;
}
