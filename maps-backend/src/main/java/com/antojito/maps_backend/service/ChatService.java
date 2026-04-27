package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.ChatResponse;
import com.antojito.maps_backend.dto.ConversationHistoryResponse;
import com.antojito.maps_backend.model.ChatConversation;
import com.antojito.maps_backend.repository.ChatRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
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

@Service
public class ChatService {

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
    private final ChatRepository chatRepository;

    private final ConcurrentHashMap<String, Conversation> conversations = new ConcurrentHashMap<>();

    public ChatService(ObjectMapper objectMapper, ChatRepository chatRepository) {
        this.objectMapper = objectMapper;
        this.restTemplate = new RestTemplate();
        this.chatRepository = chatRepository;
    }

    @PostConstruct
    public void init() {
        loadSystemPrompt();
        loadConversationsFromFile();
    }

    public ChatResponse chat(String conversationId, String userMessage) {

        if (conversationId == null || conversationId.isBlank()) {
            conversationId = UUID.randomUUID().toString();
        }

        Conversation conversation = conversations.computeIfAbsent(
                conversationId,
                id -> new Conversation(id, Instant.now().toString(), new ArrayList<>())
        );

        conversation.messages.add(
                new ConversationMessage("user", userMessage, Instant.now().toString())
        );

        List<Map<String, String>> messages = buildMistralMessages(conversation);
        String reply = callMistralApi(messages);

        conversation.messages.add(
                new ConversationMessage("assistant", reply, Instant.now().toString())
        );

        saveConversationsToFile();

        ChatConversation mongoConversation = chatRepository
                .findByConversationId(conversationId)
                .orElse(new ChatConversation());

        mongoConversation.setConversationId(conversationId);

        if (mongoConversation.getCreatedAt() == null) {
            mongoConversation.setCreatedAt(conversation.createdAt);
        }

        List<com.antojito.maps_backend.model.Message> mongoMessages =
                conversation.messages.stream()
                        .map(m -> new com.antojito.maps_backend.model.Message(
                                m.role,
                                m.content,
                                m.timestamp
                        ))
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
        System.out.println("API KEY: " + apiKey);
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
            Map<String, String> message =
                    (Map<String, String>) firstChoice.get("message");

            return message.get("content");

        } catch (Exception e) {
            return "Respuesta simulada ";
        }
    }

    private List<Map<String, String>> buildMistralMessages(Conversation conversation) {
        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new LinkedHashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        for (ConversationMessage m : conversation.messages) {
            Map<String, String> msg = new LinkedHashMap<>();
            msg.put("role", m.role);
            msg.put("content", m.content);
            messages.add(msg);
        }

        return messages;
    }

    private void loadSystemPrompt() {
        try {
            Path path = Path.of(systemPromptFile);
            if (Files.exists(path)) {
                systemPrompt = Files.readString(path, StandardCharsets.UTF_8).trim();
            } else {
                systemPrompt = "Eres un asistente.";
            }
        } catch (IOException e) {
            systemPrompt = "Eres un asistente.";
        }
    }

    private synchronized void saveConversationsToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File(conversationsFilePath), conversations);
        } catch (IOException ignored) {}
    }

    private void loadConversationsFromFile() {
        File file = new File(conversationsFilePath);
        if (file.exists()) {
            try {
                Map<String, Conversation> loaded =
                        objectMapper.readValue(file, new TypeReference<>() {});
                conversations.putAll(loaded);
            } catch (IOException ignored) {}
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