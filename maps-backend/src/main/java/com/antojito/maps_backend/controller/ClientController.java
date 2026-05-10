package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.ClientLoginResponse;
import com.antojito.maps_backend.dto.ClientRegistryRequest;
import com.antojito.maps_backend.dto.FirebaseLoginRequest;
import com.antojito.maps_backend.model.Client;
import com.antojito.maps_backend.repository.ClientRepository;
import com.antojito.maps_backend.service.AuditLogService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/client")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RequiredArgsConstructor
@Tag(name = "Client Auth", description = "Autenticacion de clientes (con Firebase)")
public class ClientController {

    private final ClientRepository clientRepository;
    private final AuditLogService auditLogService;

    @PostMapping("/login")
    @Operation(summary = "Login de cliente con Firebase", description = "Valida token de Firebase y obtiene datos del cliente")
    public ResponseEntity<ClientLoginResponse> login(@Valid @RequestBody FirebaseLoginRequest request) {
        String email;
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            email = decodedToken.getEmail();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Firebase invalido");
        }

        Client client = clientRepository.findByMail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El cliente no esta registrado"));

        // Auditoria simple usando el mismo de login (opcional)
        auditLogService.logLogin(email);

        return ResponseEntity.ok(new ClientLoginResponse(
                client.getUuid(),
                client.getMail(),
                client.getFullName(),
                client.getPhone(),
                "login correcto"));
    }

    @PostMapping("/registry")
    @Operation(summary = "Registrar cliente con Firebase", description = "Registra un cliente extraido del token de Firebase")
    public ResponseEntity<ClientLoginResponse> registry(@Valid @RequestBody ClientRegistryRequest request) {
        String email;
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken());
            email = decodedToken.getEmail();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token de Firebase invalido");
        }

        if (clientRepository.existsByMail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un cliente con ese mail");
        }

        Client client = Client.builder()
                .uuid(UUID.randomUUID())
                .mail(email)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .build();

        clientRepository.save(client);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ClientLoginResponse(
                client.getUuid(),
                client.getMail(),
                client.getFullName(),
                client.getPhone(),
                "cliente registrado"));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout de cliente", description = "Registra el cierre de sesion")
    public ResponseEntity<ApiMessageResponse> logout(@RequestBody java.util.Map<String, String> payload) {
        String email = payload.get("mail");
        if (email != null) {
            auditLogService.logLogout(email);
        }
        return ResponseEntity.ok(new ApiMessageResponse("logout registrado"));
    }
}
