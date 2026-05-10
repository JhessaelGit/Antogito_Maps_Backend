package com.antojito.maps_backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClientRegistryRequest {

    @NotBlank(message = "El token de Firebase es requerido")
    private String idToken;

    @NotBlank(message = "El nombre completo es requerido")
    private String fullName;

    @NotBlank(message = "El telefono es requerido")
    private String phone;
}
