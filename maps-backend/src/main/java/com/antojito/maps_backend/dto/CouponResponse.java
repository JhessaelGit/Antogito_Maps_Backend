package com.antojito.maps_backend.dto;

import com.antojito.maps_backend.model.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
@Schema(description = "Respuesta de cupon")
public class CouponResponse {

    @Schema(description = "UUID del cupon", example = "6f03af25-8da3-4258-b0b6-16e82fd417f0")
    private UUID uuid;

    @Schema(description = "UUID del restaurante propietario")
    private UUID restaurantId;

    @Schema(description = "UUID del cliente asignado, si aplica")
    private UUID clientId;

    @Schema(description = "Nombre del cupon")
    private String name;

    @Schema(description = "Descripcion del cupon")
    private String description;

    @Schema(description = "Fecha de inicio de vigencia")
    private LocalDate startDate;

    @Schema(description = "Fecha de expiracion")
    private LocalDate expirationDate;

    @Schema(description = "Cantidad maxima disponible")
    private Integer maxQuantity;

    @Schema(description = "Tipo de descuento")
    private String discountType;

    @Schema(description = "Estado del cupon")
    private CouponStatus status;

    @Schema(description = "Codigo QR del cupon")
    private String qrCode;

    @Schema(description = "Fecha de creacion")
    private LocalDateTime createdAt;
}
