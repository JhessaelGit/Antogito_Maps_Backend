package com.antojito.maps_backend.repository;

import com.antojito.maps_backend.model.ChatConversation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatRepository extends MongoRepository<ChatConversation, String> {

    Optional<ChatConversation> findByConversationId(String conversationId);
}