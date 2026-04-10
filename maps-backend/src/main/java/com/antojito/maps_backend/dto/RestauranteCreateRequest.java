package com.antojito.maps_backend.dto;

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
public class RestauranteCreateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    private String nombre;

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El correo no tiene formato valido")
    @Size(max = 150, message = "El correo no puede exceder 150 caracteres")
    private String correo;

    @NotBlank(message = "La contrasena es obligatoria")
    @Size(max = 255, message = "La contrasena no puede exceder 255 caracteres")
    private String contrasena;

    @NotNull(message = "La latitud es obligatoria")
    private Double lat;

    @NotNull(message = "La longitud es obligatoria")
    private Double lng;

    @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
    private String descripcion;

    @Size(max = 500, message = "La URL de imagen no puede exceder 500 caracteres")
    private String imagenUrl;

    @Size(max = 60, message = "El plan no puede exceder 60 caracteres")
    private String planSuscripcion;

    private LocalDate fechaVencimientoPlan;

    private Boolean estadoBloqueo;
}
