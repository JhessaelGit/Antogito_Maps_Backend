package com.antojito.maps_backend.dto;

import com.antojito.maps_backend.model.ComplaintType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import lombok.Data;

@Data
public class ComplaintCreateRequest {

    @NotNull(message = "El tipo de queja es requerido")
    private ComplaintType type;

    @NotNull(message = "El UUID objetivo es requerido")
    private UUID targetUuid;

    @NotBlank(message = "La descripcion no puede estar vacia")
    @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
    private String description;
}
