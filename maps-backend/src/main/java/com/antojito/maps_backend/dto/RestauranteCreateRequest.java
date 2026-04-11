package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
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
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene formato valido")
    @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
    @Schema(description = "Correo unico del restaurante", example = "nuevo@antojitos.com")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(max = 255, message = "La contrasena no puede exceder 255 caracteres")
    @Schema(description = "Contrasena de acceso", example = "Secret123!")
    private String contrasena;

    @NotNull(message = "La latitud es obligatoria")
    @Schema(description = "Latitud para geolocalizacion", example = "-17.4")
    private Double lat;

    @NotNull(message = "La longitud es obligatoria")
    @Schema(description = "Longitud para geolocalizacion", example = "-66.1")
    private Double lng;

    @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
    @Schema(description = "Descripcion comercial", example = "Registro de prueba")
    private String descripcion;

    @Size(max = 500, message = "La URL de imagen no puede exceder 500 caracteres")
    @Schema(description = "URL de la imagen del restaurante", example = "https://imagedelivery.net/.../public")
    private String imagenUrl;

    @Size(max = 60, message = "El plan no puede exceder 60 caracteres")
    @Schema(description = "Plan de suscripcion activo", example = "PREMIUM")
    private String planSuscripcion;

    @Schema(description = "Fecha de vencimiento del plan", example = "2026-12-31")
    private LocalDate fechaVencimientoPlan;

    @Schema(description = "Estado de bloqueo de la cuenta", example = "false")
    private Boolean estadoBloqueo;
}
