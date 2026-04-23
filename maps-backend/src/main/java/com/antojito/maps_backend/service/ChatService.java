package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.ChatResponse;
import com.antojito.maps_backend.dto.ConversationHistoryResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    @Value("${app.mistral.api-key}")
    private String apiKey;

    @Value("${app.mistral.api-url}")
    private String apiUrl;

    @Value("${app.mistral.model}")
    private String model;

    @Value("${app.chat.system-prompt-file:system_prompt.txt}")
    private String systemPromptFile;

    @Value("${app.chat.conversations-file:conversations.json}")
    private String conversationsFilePath;

    private String systemPrompt;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Mapa en memoria: conversationId -> conversacion completa.
     * Se sincroniza con el archivo JSON en cada escritura.
     */
    private final ConcurrentHashMap<String, Conversation> conversations = new ConcurrentHashMap<>();

    public ChatService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        loadSystemPrompt();
        loadConversationsFromFile();
    }

    public ChatResponse chat(String conversationId, String userMessage) {
        validateApiKey();

        // Si no se envio conversationId, crear nueva conversacion
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        // Obtener o crear la conversacion
        Conversation conversation = conversations.computeIfAbsent(
                conversationId,
                id -> new Conversation(id, Instant.now().toString(), new ArrayList<>()));

        // Agregar mensaje del usuario
        conversation.messages.add(new Message("user", userMessage, Instant.now().toString()));

        // Construir la lista de mensajes para Mistral (con system prompt)
        List<Map<String, String>> mistralMessages = buildMistralMessages(conversation);

        // Llamar a Mistral API
        String aiReply = callMistralApi(mistralMessages);

        // Agregar respuesta de IA a la conversacion
        conversation.messages.add(new Message("assistant", aiReply, Instant.now().toString()));

        // Persistir
        saveConversationsToFile();

        return ChatResponse.builder()
                .conversationId(conversationId)
                .reply(aiReply)
                .build();
    }

    public ConversationHistoryResponse getConversation(String conversationId) {
        Conversation conversation = conversations.get(conversationId);
        if (conversation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No existe conversacion con id " + conversationId);
        }

        List<ConversationHistoryResponse.MessageEntry> entries = conversation.messages.stream()
                .map(m -> ConversationHistoryResponse.MessageEntry.builder()
                        .role(m.role)
                        .content(m.content)
                        .timestamp(m.timestamp)
                        .build())
                .toList();

        return ConversationHistoryResponse.builder()
                .conversationId(conversation.id)
                .createdAt(conversation.createdAt)
                .messages(entries)
                .build();
    }

    public List<Map<String, Object>> listConversations() {
        return conversations.values().stream()
                .sorted(Comparator.comparing((Conversation c) -> c.createdAt).reversed())
                .map(c -> {
                    Map<String, Object> summary = new LinkedHashMap<>();
                    summary.put("conversationId", c.id);
                    summary.put("createdAt", c.createdAt);
                    summary.put("messageCount", c.messages.size());
                    // primer mensaje del usuario como preview
                    c.messages.stream()
                            .filter(m -> "user".equals(m.role))
                            .findFirst()
                            .ifPresent(m -> summary.put("preview", truncate(m.content, 80)));
                    return summary;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private String callMistralApi(List<Map<String, String>> messages) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 1024);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                    return message.get("content");
                }
            }

            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Respuesta inesperada de Mistral AI");

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error al comunicarse con Mistral AI: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Error al comunicarse con el modelo de IA: " + e.getMessage());
        }
    }

    private List<Map<String, String>> buildMistralMessages(Conversation conversation) {
        List<Map<String, String>> messages = new ArrayList<>();

        // System prompt
        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        // Historial de la conversacion
        for (Message m : conversation.messages) {
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("role", m.role);
            msg.put("content", m.content);
            messages.add(msg);
        }

        return messages;
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "API Key de Mistral AI no configurada. Configure app.mistral.api-key");
        }
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "...";
    }

    private void loadSystemPrompt() {
        try {
            Path path = Path.of(systemPromptFile);
            if (Files.exists(path)) {
                systemPrompt = Files.readString(path, StandardCharsets.UTF_8).trim();
                logger.info("System prompt cargado desde {}", systemPromptFile);
            } else {
                systemPrompt = "Eres un asistente de Antojitos Maps. Responde en espanol.";
                logger.warn("Archivo de system prompt no encontrado ({}), usando prompt por defecto", systemPromptFile);
            }
        } catch (IOException e) {
            systemPrompt = "Eres un asistente de Antojitos Maps. Responde en espanol.";
            logger.error("Error al leer system prompt: {}", e.getMessage());
        }
    }

    private synchronized void saveConversationsToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(conversationsFilePath), conversations);
            logger.debug("Conversaciones guardadas en {}", conversationsFilePath);
        } catch (IOException e) {
            logger.error("Error al guardar conversaciones: {}", e.getMessage());
        }
    }

    private void loadConversationsFromFile() {
        File file = new File(conversationsFilePath);
        if (file.exists() && file.length() > 0) {
            try {
                Map<String, Conversation> loaded = objectMapper.readValue(
                        file,
                        new TypeReference<Map<String, Conversation>>() {});
                conversations.putAll(loaded);
                logger.info("Se cargaron {} conversaciones desde {}", loaded.size(), conversationsFilePath);
            } catch (IOException e) {
                logger.warn("No se pudieron cargar conversaciones existentes: {}", e.getMessage());
            }
        }
    }

    /**
     * Modelo de una conversacion completa.
     */
    public static class Conversation {
        public String id;
        public String createdAt;
        public List<Message> messages;

        public Conversation() {}

        public Conversation(String id, String createdAt, List<Message> messages) {
            this.id = id;
            this.createdAt = createdAt;
            this.messages = messages;
        }
    }

    /**
     * Modelo de un mensaje individual.
     */
    public static class Message {
        public String role;
        public String content;
        public String timestamp;

        public Message() {}

        public Message(String role, String content, String timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}
