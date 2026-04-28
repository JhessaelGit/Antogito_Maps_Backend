package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.ChatResponse;
import com.antojito.maps_backend.dto.ConversationHistoryResponse;
import com.antojito.maps_backend.model.ChatConversation;
import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.ChatRepository;
import com.antojito.maps_backend.repository.RestauranteRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@ConditionalOnExpression("'${app.mistral.api-key:}' != ''")
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

    @Value("${app.chat.context-file:context.json}")
    private String contextFile;

    @Value("${app.chat.conversations-file:conversations.json}")
    private String conversationsFilePath;

    private String systemPrompt;
    private String contextJson;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final ChatRepository chatRepository;
    private final RestauranteRepository restauranteRepository;

    private final ConcurrentHashMap<String, Conversation> conversations = new ConcurrentHashMap<>();

    public ChatService(ObjectMapper objectMapper, ChatRepository chatRepository,
            RestauranteRepository restauranteRepository) {
        this.objectMapper = objectMapper;
        this.chatRepository = chatRepository;
        this.restauranteRepository = restauranteRepository;
        this.restTemplate = new RestTemplate();
    }

    @PostConstruct
    public void init() {
        loadSystemPrompt();
        loadContext();
        loadConversationsFromFile();
    }

    public ChatResponse chat(String conversationId, String userMessage, Double latitude, Double longitude) {
        validateApiKey();

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        Conversation conversation = conversations.computeIfAbsent(
                conversationId,
                id -> new Conversation(id, Instant.now().toString(), new ArrayList<>()));

        conversation.messages.add(new ConversationMessage("user", userMessage, Instant.now().toString()));

        List<Map<String, String>> mistralMessages = buildMistralMessages(conversation, latitude, longitude);
        String reply = callMistralApi(mistralMessages);

        conversation.messages.add(new ConversationMessage("assistant", reply, Instant.now().toString()));

        saveConversationsToFile();

        // Persist to MongoDB
        ChatConversation mongoConversation = chatRepository
                .findByConversationId(conversationId)
                .orElse(new ChatConversation());

        mongoConversation.setConversationId(conversationId);

        if (mongoConversation.getCreatedAt() == null) {
            mongoConversation.setCreatedAt(conversation.createdAt);
        }

        List<com.antojito.maps_backend.model.Message> mongoMessages =
                conversation.messages.stream()
                        .map(m -> new com.antojito.maps_backend.model.Message(m.role, m.content, m.timestamp))
                        .toList();

        mongoConversation.setMessages(mongoMessages);
        chatRepository.save(mongoConversation);

        return ChatResponse.builder()
                .conversationId(conversationId)
                .reply(reply)
                .build();
    }

    public ConversationHistoryResponse getConversation(String conversationId) {
        Conversation conversation = conversations.get(conversationId);

        if (conversation == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        List<ConversationHistoryResponse.MessageEntry> entries =
                conversation.messages.stream()
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
                    return summary;
                })
                .toList();
    }

    private String callMistralApi(List<Map<String, String>> messages) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.getBody().get("choices");

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, String> message = (Map<String, String>) firstChoice.get("message");

            return message.get("content");

        } catch (Exception e) {
            logger.error("Error al llamar Mistral API: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Error al comunicarse con Mistral AI: " + e.getMessage());
        }
    }

    private List<Map<String, String>> buildMistralMessages(Conversation conversation,
            Double latitude, Double longitude) {
        List<Map<String, String>> messages = new ArrayList<>();

        StringBuilder systemContent = new StringBuilder();
        systemContent.append(systemPrompt);
        if (contextJson != null && !contextJson.isBlank()) {
            systemContent.append("\n\nCONTEXTO ESTRUCTURADO (JSON):\n");
            systemContent.append(contextJson);
        }

        String nearbyInfo = buildNearbyRestaurantsContext(latitude, longitude);
        if (nearbyInfo != null) {
            systemContent.append("\n\n").append(nearbyInfo);
        }

        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemContent.toString());
        messages.add(systemMsg);

        for (ConversationMessage m : conversation.messages) {
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("role", m.role);
            msg.put("content", m.content);
            messages.add(msg);
        }

        return messages;
    }

    private static final double RADIO_CERCANO_KM = 5.0;

    private String buildNearbyRestaurantsContext(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        try {
            List<Restaurante> allRestaurants = restauranteRepository.findAll().stream()
                    .filter(r -> r.getIsBlocked() == null || !r.getIsBlocked())
                    .toList();

            if (allRestaurants.isEmpty()) {
                return "RESTAURANTES CERCANOS: No hay restaurantes registrados en la plataforma actualmente.";
            }

            List<Map<String, Object>> nearby = allRestaurants.stream()
                    .map(r -> {
                        double distance = haversineDistance(latitude, longitude,
                                r.getLatitude(), r.getLongitude());
                        Map<String, Object> info = new LinkedHashMap<>();
                        info.put("nombre", r.getName());
                        info.put("categoria", r.getCategory() != null ? r.getCategory() : "Sin categoría");
                        info.put("descripcion", r.getDescription() != null ? r.getDescription() : "Sin descripción");
                        info.put("distancia_km", Math.round(distance * 100.0) / 100.0);
                        return info;
                    })
                    .filter(m -> (Double) m.get("distancia_km") <= RADIO_CERCANO_KM)
                    .sorted(Comparator.comparingDouble(m -> (Double) m.get("distancia_km")))
                    .collect(Collectors.toList());

            StringBuilder sb = new StringBuilder();
            sb.append("UBICACIÓN DEL USUARIO: lat=").append(latitude)
              .append(", lng=").append(longitude).append("\n");

            if (nearby.isEmpty()) {
                sb.append("RESTAURANTES CERCANOS: No se encontraron restaurantes dentro de un radio de ")
                  .append(RADIO_CERCANO_KM)
                  .append(" km. Informa al usuario que no hay restaurantes cerca de su ubicación actual.");
            } else {
                sb.append("RESTAURANTES DENTRO DE ").append(RADIO_CERCANO_KM)
                  .append(" KM (SOLO recomienda estos, NO inventes otros):\n");
                for (int i = 0; i < nearby.size(); i++) {
                    Map<String, Object> r = nearby.get(i);
                    sb.append(String.format("%d. %s (%s) - %s - a %.2f km\n",
                            i + 1, r.get("nombre"), r.get("categoria"),
                            r.get("descripcion"), r.get("distancia_km")));
                }
            }

            logger.info("Contexto de restaurantes cercanos: {} encontrados en radio de {} km",
                    nearby.size(), RADIO_CERCANO_KM);
            return sb.toString();

        } catch (Exception e) {
            logger.error("Error al consultar restaurantes cercanos: {}", e.getMessage());
            return null;
        }
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "API Key de Mistral AI no configurada. Configure app.mistral.api-key");
        }
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

    private void loadContext() {
        try {
            Path path = Path.of(contextFile);
            if (Files.exists(path)) {
                contextJson = Files.readString(path, StandardCharsets.UTF_8).trim();
                logger.info("Contexto estructurado cargado desde {}", contextFile);
            } else {
                contextJson = null;
                logger.warn("Archivo de contexto no encontrado ({}), el chatbot funcionará sin contexto estructurado",
                        contextFile);
            }
        } catch (IOException e) {
            contextJson = null;
            logger.error("Error al leer contexto estructurado: {}", e.getMessage());
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
                Map<String, Conversation> loaded =
                        objectMapper.readValue(file, new TypeReference<Map<String, Conversation>>() {});
                conversations.putAll(loaded);
                logger.info("Se cargaron {} conversaciones desde {}", loaded.size(), conversationsFilePath);
            } catch (IOException e) {
                logger.warn("No se pudieron cargar conversaciones existentes: {}", e.getMessage());
            }
        }
    }

    public static class Conversation {
        public String id;
        public String createdAt;
        public List<ConversationMessage> messages;

        public Conversation() {}

        public Conversation(String id, String createdAt, List<ConversationMessage> messages) {
            this.id = id;
            this.createdAt = createdAt;
            this.messages = messages;
        }
    }

    public static class ConversationMessage {
        public String role;
        public String content;
        public String timestamp;

        public ConversationMessage() {}

        public ConversationMessage(String role, String content, String timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
    }
}
