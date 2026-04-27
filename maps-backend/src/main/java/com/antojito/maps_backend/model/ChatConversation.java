package com.antojito.maps_backend.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "conversations")
public class ChatConversation {

    @Id
    private String id;

    private String conversationId;
    private String createdAt;

    private List<Message> messages;
}