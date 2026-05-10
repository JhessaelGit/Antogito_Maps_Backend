package com.antojito.maps_backend.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientLoginResponse {
    private UUID uuid;
    private String mail;
    private String fullName;
    private String phone;
    private String message;
}
