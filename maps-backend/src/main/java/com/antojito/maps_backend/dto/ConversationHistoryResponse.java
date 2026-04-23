package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Historial completo de una conversacion")
public class ConversationHistoryResponse {

    @Schema(description = "UUID de la conversacion")
    private String conversationId;

    @Schema(description = "Fecha de creacion de la conversacion")
    private String createdAt;

    @Schema(description = "Lista de mensajes en la conversacion")
    private List<MessageEntry> messages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Entrada individual de mensaje")
    public static class MessageEntry {

        @Schema(description = "Rol del mensaje: user o assistant")
        private String role;

        @Schema(description = "Contenido del mensaje")
        private String content;

        @Schema(description = "Timestamp del mensaje")
        private String timestamp;
    }
}
