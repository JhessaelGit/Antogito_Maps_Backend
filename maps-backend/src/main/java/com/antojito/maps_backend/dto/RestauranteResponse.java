package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Respuesta de restaurante")
public class RestauranteResponse {

    @Schema(description = "ID del restaurante", example = "11")
    private Long id;

    @Schema(description = "Nombre comercial", example = "Sabor Valluno")
    private String nombre;

    @Schema(description = "Correo del restaurante", example = "sabor.valluno@antojitosmaps.com")
    private String correo;

    @Schema(description = "Descripcion del restaurante", example = "Comida tipica cochabambina")
    private String descripcion;

    @Schema(description = "URL de imagen de portada", example = "https://imagedelivery.net/.../public")
    private String imagenUrl;

    @Schema(description = "Plan de suscripcion", example = "PREMIUM")
    private String planSuscripcion;

    @Schema(description = "Fecha de vencimiento del plan", example = "2026-10-10")
    private LocalDate fechaVencimientoPlan;

    @Schema(description = "Estado de bloqueo", example = "false")
    private Boolean estadoBloqueo;

    @Schema(description = "Latitud", example = "-17.3922")
    private Double lat;

    @Schema(description = "Longitud", example = "-66.1561")
    private Double lng;
}
