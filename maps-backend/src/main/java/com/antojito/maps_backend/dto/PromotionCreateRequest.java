package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
@Schema(description = "Payload para crear una promocion")
public class PromotionCreateRequest {

    @NotBlank(message = "El mail del owner es obligatorio")
    @Email(message = "El mail del owner no tiene formato valido")
    @Size(max = 150, message = "El mail del owner no puede exceder 150 caracteres")
    @Schema(description = "Mail del owner autenticado", example = "owner.sabor@antojitosmaps.com")
    private String ownerMail;

    @NotBlank(message = "El titulo es obligatorio")
    @Size(max = 120, message = "El titulo no puede exceder 120 caracteres")
    @Schema(description = "Titulo de la promocion", example = "2x1 en saltenas")
    private String title;

    @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
    @Schema(description = "Descripcion comercial de la promocion", example = "Valido de lunes a viernes de 10:00 a 12:00")
    private String description;

    @NotNull(message = "El porcentaje de descuento es obligatorio")
    @DecimalMin(value = "0.0", message = "El porcentaje debe ser mayor o igual a 0")
    @DecimalMax(value = "100.0", message = "El porcentaje debe ser menor o igual a 100")
    @Schema(description = "Porcentaje de descuento", example = "25.0")
    private BigDecimal percentDiscount;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Schema(description = "Fecha de inicio de vigencia (formato yyyy-MM-dd)", example = "2026-04-20")
    private LocalDate dateStartPromotion;

    @NotNull(message = "La fecha de fin es obligatoria")
    @Schema(description = "Fecha de fin de vigencia (formato yyyy-MM-dd)", example = "2026-04-30")
    private LocalDate dateEndPromotion;

    @Schema(description = "Estado activo de la promocion", example = "true")
    private Boolean isActivePromotion;
}
