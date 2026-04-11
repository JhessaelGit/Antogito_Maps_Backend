package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@Schema(description = "Payload para registrar owner en un restaurante")
public class RestaurantRegistryRequest {

    @NotNull(message = "El id del restaurante es obligatorio")
    @Schema(description = "UUID del restaurante", example = "5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9")
    private UUID restaurantId;

    @NotBlank(message = "El mail es obligatorio")
    @Email(message = "El mail no tiene formato valido")
    @Size(max = 150, message = "El mail no puede exceder 150 caracteres")
    @Schema(description = "Mail del owner", example = "owner.nuevo@antojitosmaps.com")
    private String mail;

    @NotBlank(message = "La password es obligatoria")
    @Size(max = 255, message = "La password no puede exceder 255 caracteres")
    @Schema(description = "Password del owner", example = "OwnerNuevo2026!")
    private String password;
}
