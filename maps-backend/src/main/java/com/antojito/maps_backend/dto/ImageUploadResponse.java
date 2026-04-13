package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de subida de imagen")
public record ImageUploadResponse(
        @Schema(description = "URL publica en Cloudflare R2", example = "https://<account-id>.r2.cloudflarestorage.com/<bucket>/restaurantes/foto.jpg")
        String imageUrl) {
}
