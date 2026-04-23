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
@Schema(description = "Respuesta del chatbot")
public class ChatResponse {

    @Schema(description = "UUID de la conversacion", example = "f47ac10b-58cc-4372-a567-0e02b2c3d479")
    private String conversationId;

    @Schema(description = "Respuesta generada por la IA", example = "Te recomiendo visitar Sabor Valluno, tienen comida tipica cochabambina.")
    private String reply;
}
