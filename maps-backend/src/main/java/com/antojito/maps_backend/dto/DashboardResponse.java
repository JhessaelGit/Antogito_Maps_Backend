package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Respuesta del dashboard analitico")
public class DashboardResponse {

    @Schema(description = "Cantidad de clientes con puntos acumulados", example = "42")
    private Long totalClientsWithPoints;

    @Schema(description = "Cantidad de usuarios en nivel BRONCE", example = "20")
    private Long bronzeUsers;

    @Schema(description = "Cantidad de usuarios en nivel PLATA", example = "15")
    private Long silverUsers;

    @Schema(description = "Cantidad de usuarios en nivel ORO", example = "7")
    private Long goldUsers;

    @Schema(description = "Total de puntos entregados en el sistema", example = "9800")
    private Long totalPointsDelivered;
}
