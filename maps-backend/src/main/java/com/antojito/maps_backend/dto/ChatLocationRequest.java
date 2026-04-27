package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para vincular la ubicacion del usuario a una conversacion")
public class ChatLocationRequest {

    @NotBlank(message = "El conversationId no puede estar vacio")
    @Schema(description = "UUID de la conversacion a la que se vincula la ubicacion", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String conversationId;

    @NotNull(message = "La latitud es requerida")
    @Schema(description = "Latitud actual del usuario", example = "-17.3935")
    private Double latitude;

    @NotNull(message = "La longitud es requerida")
    @Schema(description = "Longitud actual del usuario", example = "-66.1570")
    private Double longitude;
}
