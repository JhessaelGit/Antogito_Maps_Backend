package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
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
@Schema(description = "Respuesta de promocion")
public class PromotionResponse {

    @Schema(description = "UUID de la promocion", example = "6f03af25-8da3-4258-b0b6-16e82fd417f0")
    private UUID uuid;

    @Schema(description = "UUID del restaurante propietario", example = "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9")
    private UUID restaurantId;

    @Schema(description = "Titulo de la promocion", example = "2x1 en saltenas")
    private String title;

    @Schema(description = "Descripcion de la promocion", example = "Valido de lunes a viernes")
    private String description;

    @Schema(description = "Porcentaje de descuento", example = "25.0")
    private BigDecimal percentDiscount;

    @Schema(description = "Fecha inicio vigencia (yyyy-MM-dd)", example = "2026-04-20")
    private LocalDate dateStartPromotion;

    @Schema(description = "Fecha fin vigencia (yyyy-MM-dd)", example = "2026-04-30")
    private LocalDate dateEndPromotion;

    @Schema(description = "Estado activo de la promocion", example = "true")
    private Boolean isActivePromotion;
}
