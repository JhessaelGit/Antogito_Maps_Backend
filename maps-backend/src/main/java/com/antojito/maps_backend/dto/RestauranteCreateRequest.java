package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
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
@Schema(description = "Payload para crear un restaurante")
public class RestauranteCreateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    @Schema(description = "Nombre comercial del restaurante", example = "Nuevo Antojito")
    private String name;

    @NotNull(message = "La latitud es obligatoria")
    @Schema(description = "Latitud para geolocalizacion", example = "-17.4")
    private Double latitude;

    @NotNull(message = "La longitud es obligatoria")
    @Schema(description = "Longitud para geolocalizacion", example = "-66.1")
    private Double longitude;

    @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
    @Schema(description = "Descripcion comercial", example = "Registro de prueba")
    private String description;

    @Size(max = 500, message = "La URL de imagen no puede exceder 500 caracteres")
    @Schema(description = "URL de la imagen del restaurante", example = "https://d32...r2.cloudflarestorage.com/antojitos-maps-images/restaurantes/foto.jpg")
    private String imagenUrl;

    @Size(max = 60, message = "El plan no puede exceder 60 caracteres")
    @Schema(description = "Plan de suscripcion activo", example = "PREMIUM")
    private String planSuscription;

    @Schema(description = "Fecha de vencimiento del plan", example = "2026-12-31")
    private LocalDate planExpirationDate;

    @Schema(description = "Estado de bloqueo de la cuenta", example = "false")
    private Boolean isBlocked;

    @Size(max = 80, message = "La categoria no puede exceder 80 caracteres")
    @Schema(description = "Categoria del restaurante", example = "Comida Rapida")
    private String category;
}
