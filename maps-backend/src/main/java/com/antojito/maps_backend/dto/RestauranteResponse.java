package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;
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
@Schema(description = "Respuesta de restaurante")
public class RestauranteResponse {

    @Schema(description = "UUID del restaurante", example = "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9")
    private UUID uuid;

    @Schema(description = "Nombre comercial", example = "Sabor Valluno")
    private String name;

    @Schema(description = "Descripcion del restaurante", example = "Comida tipica cochabambina")
    private String description;

    @Schema(description = "URL de imagen de portada", example = "https://d32...r2.cloudflarestorage.com/antojitos-maps-images/restaurantes/foto.jpg")
    private String imagenUrl;

    @Schema(description = "Plan de suscripcion", example = "PREMIUM")
    private String planSuscription;

    @Schema(description = "Fecha de vencimiento del plan", example = "2026-10-10")
    private LocalDate planExpirationDate;

    @Schema(description = "Estado de bloqueo", example = "false")
    private Boolean isBlocked;

    @Schema(description = "Latitud", example = "-17.3922")
    private Double latitude;

    @Schema(description = "Longitud", example = "-66.1561")
    private Double longitude;

    @Schema(description = "Categoria", example = "Comida Tipica")
    private String category;
}
