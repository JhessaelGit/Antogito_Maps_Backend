package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ChatRequest;
import com.antojito.maps_backend.dto.ChatResponse;
import com.antojito.maps_backend.dto.ConversationHistoryResponse;
import com.antojito.maps_backend.service.ChatService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@ConditionalOnBean(ChatService.class)
@Tag(name = "Chatbot", description = "Endpoints de chatbot con IA (Mistral AI)")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(
            summary = "Enviar mensaje al chatbot",
            description = "Envia un mensaje del usuario al modelo de IA y devuelve la respuesta. "
                    + "Si no se envia conversationId, se usa cookie o se crea una nueva conversacion con UUID. "
                    + "Si se envian latitude y longitude, el chatbot recomendara restaurantes cercanos.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Respuesta generada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"conversationId\":\"f47ac10b-58cc-4372-a567-0e02b2c3d479\",\"reply\":\"Te recomiendo visitar Sabor Valluno.\"}")
                    )
            ),
            @ApiResponse(responseCode = "400", description = "Mensaje vacio o request invalido"),
            @ApiResponse(responseCode = "502", description = "Error al comunicarse con Mistral AI")
    })
    public ResponseEntity<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            HttpServletResponse response,
            @CookieValue(value = "conversationId", required = false) String cookieConversationId
    ) {
        String conversationId = request.getConversationId();

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = cookieConversationId;
        }

        ChatResponse chatResponse = chatService.chat(
                conversationId,
                request.getMessage(),
                request.getLatitude(),
                request.getLongitude());

        Cookie cookie = new Cookie("conversationId", chatResponse.getConversationId());
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24);
        response.addCookie(cookie);

        return ResponseEntity.ok(chatResponse);
    }

    @GetMapping("/{conversationId}")
    @Operation(
            summary = "Obtener historial de conversacion",
            description = "Devuelve el historial completo de una conversacion por su UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial encontrado"),
            @ApiResponse(responseCode = "404", description = "Conversacion no encontrada")
    })
    public ResponseEntity<ConversationHistoryResponse> getConversation(
            @PathVariable String conversationId) {
        return ResponseEntity.ok(chatService.getConversation(conversationId));
    }

    @GetMapping("/conversations")
    @Operation(
            summary = "Listar conversaciones",
            description = "Devuelve un resumen de todas las conversaciones almacenadas.")
    @ApiResponse(responseCode = "200", description = "Lista de conversaciones")
    public ResponseEntity<List<Map<String, Object>>> listConversations() {
        return ResponseEntity.ok(chatService.listConversations());
    }
}
