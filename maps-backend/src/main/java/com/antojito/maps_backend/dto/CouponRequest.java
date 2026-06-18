package com.antojito.maps_backend.dto;

import com.antojito.maps_backend.model.CouponStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Payload para crear o editar cupones")
public class CouponRequest {

    @Schema(description = "UUID del owner autenticado (preferido)", example = "20a63174-3799-4e7f-98c7-7f2af9e2c42c")
    private UUID ownerUuid;

    @Email(message = "El mail del owner no tiene formato valido")
    @Size(max = 150, message = "El mail del owner no puede exceder 150 caracteres")
    @Schema(description = "Mail del owner autenticado (alternativo si no envias ownerUuid)", example = "owner.sabor@antojitosmaps.com")
    private String ownerMail;

    @Schema(description = "UUID del cliente al que se asigna el cupon, si aplica")
    private UUID clientId;

    @NotBlank(message = "El nombre del cupon es obligatorio")
    @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
    @Schema(description = "Nombre del cupon", example = "Descuento de bienvenida")
    private String name;

    @Size(max = 500, message = "La descripcion no puede exceder 500 caracteres")
    @Schema(description = "Descripcion del cupon", example = "Valido en compras mayores a 50 Bs")
    private String description;

    @NotNull(message = "La fecha de inicio es obligatoria")
    @Schema(description = "Fecha de inicio de vigencia (yyyy-MM-dd)", example = "2026-06-20")
    private LocalDate startDate;

    @NotNull(message = "La fecha de expiracion es obligatoria")
    @Schema(description = "Fecha de expiracion de vigencia (yyyy-MM-dd)", example = "2026-06-30")
    private LocalDate expirationDate;

    @NotNull(message = "La cantidad maxima es obligatoria")
    @Min(value = 1, message = "La cantidad maxima debe ser mayor a 0")
    @Schema(description = "Cantidad maxima disponible", example = "100")
    private Integer maxQuantity;

    @NotBlank(message = "El tipo de descuento es obligatorio")
    @Size(max = 50, message = "El tipo de descuento no puede exceder 50 caracteres")
    @Schema(description = "Tipo de descuento", example = "PERCENTAGE")
    private String discountType;

    @Schema(description = "Estado del cupon", example = "ACTIVE")
    private CouponStatus status;

    @Size(max = 500, message = "El codigo QR no puede exceder 500 caracteres")
    @Schema(description = "Codigo QR del cupon")
    private String qrCode;
}
