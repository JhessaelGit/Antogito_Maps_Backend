package com.antojito.maps_backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.UUID;
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
@Schema(description = "Payload para validar permisos del owner")
public class OwnerAuthorizationRequest {

    @Schema(description = "UUID del owner autenticado (preferido)", example = "20a63174-3799-4e7f-98c7-7f2af9e2c42c")
    private UUID ownerUuid;

    @Email(message = "El mail del owner no tiene formato valido")
    @Size(max = 150, message = "El mail del owner no puede exceder 150 caracteres")
    @Schema(description = "Mail del owner autenticado (alternativo si no envias ownerUuid)", example = "owner.sabor@antojitosmaps.com")
    private String ownerMail;
}
