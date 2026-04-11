package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta simple de mensaje")
public record ApiMessageResponse(String message) {
}
