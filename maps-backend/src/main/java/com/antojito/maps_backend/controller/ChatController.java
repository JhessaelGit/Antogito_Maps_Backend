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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "Endpoints de chatbot con IA (Mistral AI)")
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    @Operation(
            summary = "Enviar mensaje al chatbot",
            description = "Envia un mensaje del usuario al modelo de IA y devuelve la respuesta. Si no se envia conversationId, se usa cookie."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Respuesta generada exitosamente",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"conversationId\":\"uuid\",\"reply\":\"respuesta\"}")
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

        ChatResponse chatResponse = chatService.chat(conversationId, request.getMessage());

        Cookie cookie = new Cookie("conversationId", chatResponse.getConversationId());
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24);

        response.addCookie(cookie);

        return ResponseEntity.ok(chatResponse);
    }

    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationHistoryResponse> getConversation(
            @PathVariable String conversationId) {
        return ResponseEntity.ok(chatService.getConversation(conversationId));
    }

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> listConversations() {
        return ResponseEntity.ok(chatService.listConversations());
    }
}