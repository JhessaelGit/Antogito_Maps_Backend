package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
@Schema(description = "Payload para bloquear o desbloquear restaurante")
public class AdminRestaurantBlockRequest {

    @NotNull(message = "isBlocked es obligatorio")
    @Schema(description = "Nuevo estado de bloqueo", example = "true")
    private Boolean isBlocked;
}
