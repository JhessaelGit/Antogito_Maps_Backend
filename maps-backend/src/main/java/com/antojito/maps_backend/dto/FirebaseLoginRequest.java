package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payload para autenticar con Firebase (recibe idToken generado por el cliente)")
public class FirebaseLoginRequest {

    @NotBlank(message = "El token de Firebase es obligatorio")
    @Schema(description = "ID Token de Firebase devuelto al hacer login en el frontend", example = "eyJhbGciOiJSUzI1NiIs...")
    private String idToken;
}
