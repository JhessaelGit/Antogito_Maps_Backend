package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
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
@Schema(description = "Respuesta de login de owner")
public class RestaurantLoginResponse {

    @Schema(description = "UUID del owner autenticado", example = "20a63174-3799-4e7f-98c7-7f2af9e2c42c")
    private UUID ownerId;

    @Schema(description = "Mail del owner autenticado", example = "owner.sabor@antojitosmaps.com")
    private String mail;

    @Schema(
            description = "Lista de UUIDs de restaurantes asociados al owner",
            example = "[\"5ec5e321-5fa1-4a4b-9370-0d9f8cfa8ca9\",\"58f58d45-2d7c-47ff-a6ff-c0d57cb021c2\"]")
    private List<UUID> restaurantIds;

    @Schema(description = "Mensaje de resultado", example = "login correcto")
    private String message;
}
