package com.antojito.maps_backend.dto;

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
public class RestauranteResponse {

    private Long id;
    private String nombre;
    private String correo;
    private String descripcion;
    private String imagenUrl;
    private String planSuscripcion;
    private LocalDate fechaVencimientoPlan;
    private Boolean estadoBloqueo;
    private Double lat;
    private Double lng;
}
