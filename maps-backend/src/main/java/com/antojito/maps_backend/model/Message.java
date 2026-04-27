package com.antojito.maps_backend.model;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    private String role;
    private String content;
    private String timestamp;
}