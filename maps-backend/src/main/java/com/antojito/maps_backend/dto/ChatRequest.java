package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request para enviar un mensaje al chatbot")
public class ChatRequest {

    @Schema(description = "UUID de la conversacion existente (null para nueva conversacion)", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String conversationId;

    @NotBlank(message = "El mensaje no puede estar vacio")
    @Schema(description = "Mensaje del usuario", example = "Hola, que restaurantes me recomiendas?")
    private String message;

    @Schema(description = "Latitud actual del usuario (opcional, para recomendaciones cercanas)", example = "-17.3935")
    private Double latitude;

    @Schema(description = "Longitud actual del usuario (opcional, para recomendaciones cercanas)", example = "-66.1570")
    private Double longitude;
}
