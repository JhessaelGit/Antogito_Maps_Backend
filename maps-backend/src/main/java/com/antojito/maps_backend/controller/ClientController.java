package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.ClientLoginRequest;
import com.antojito.maps_backend.dto.ClientLoginResponse;
import com.antojito.maps_backend.dto.ClientRegistryRequest;
import com.antojito.maps_backend.model.Client;
import com.antojito.maps_backend.repository.ClientRepository;
import com.antojito.maps_backend.service.AuditLogService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/client")
@CrossOrigin(origins = "${app.cors.allowed-origins:*}")
@RequiredArgsConstructor
@Tag(name = "Client Auth", description = "Autenticacion de clientes (Firebase gestionado por el backend)")
public class ClientController {

    private final ClientRepository clientRepository;
    private final AuditLogService auditLogService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${firebase.api-key}")
    private String firebaseApiKey;

    // ─────────────────────────────────────────────────────────────
    // REGISTRO: crea usuario en Firebase + guarda en BD
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/registry")
    @Operation(
            summary = "Registrar cliente",
            description = "Crea el usuario en Firebase y lo registra en la base de datos. " +
                    "El frontend solo envia email, password, fullName y phone.")
    public ResponseEntity<ClientLoginResponse> registry(@Valid @RequestBody ClientRegistryRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        // 1. Verificar que no exista ya en la BD
        if (clientRepository.existsByMail(email)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un cliente con ese correo");
        }

        // 2. Crear usuario en Firebase Admin
        try {
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(email)
                    .setPassword(request.getPassword())
                    .setDisplayName(request.getFullName());
            FirebaseAuth.getInstance().createUser(createRequest);
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("EMAIL_EXISTS") || msg.contains("email-already-exists")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El correo ya esta registrado en Firebase");
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error al crear usuario en Firebase: " + msg);
        }

        // 3. Guardar en base de datos
        Client client = Client.builder()
                .uuid(UUID.randomUUID())
                .mail(email)
                .fullName(request.getFullName().trim())
                .phone(request.getPhone().trim())
                .build();
        clientRepository.save(client);

        auditLogService.logLogin(email);

        return ResponseEntity.status(HttpStatus.CREATED).body(new ClientLoginResponse(
                client.getUuid(),
                client.getMail(),
                client.getFullName(),
                client.getPhone(),
                "cliente registrado"));
    }

    // ─────────────────────────────────────────────────────────────
    // LOGIN: autentica en Firebase REST + devuelve datos del cliente
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/login")
    @Operation(
            summary = "Login de cliente",
            description = "Autentica email y password contra Firebase y devuelve los datos del cliente registrado en la BD.")
    public ResponseEntity<ClientLoginResponse> login(@Valid @RequestBody ClientLoginRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        // 1. Autenticar contra Firebase REST Identity Toolkit
        String firebaseUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;
        try {
            Map<String, Object> body = Map.of(
                    "email", email,
                    "password", request.getPassword(),
                    "returnSecureToken", true);
            restTemplate.postForEntity(firebaseUrl, body, Map.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contrasena incorrectos");
        }

        // 2. Buscar en BD
        Client client = clientRepository.findByMail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "El correo no esta registrado como cliente"));

        auditLogService.logLogin(email);

        return ResponseEntity.ok(new ClientLoginResponse(
                client.getUuid(),
                client.getMail(),
                client.getFullName(),
                client.getPhone(),
                "login correcto"));
    }

    // ─────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────
    @PostMapping("/logout")
    @Operation(summary = "Logout de cliente", description = "Registra el cierre de sesion del cliente")
    public ResponseEntity<ApiMessageResponse> logout(@RequestBody Map<String, String> payload) {
        String email = payload.get("mail");
        if (email != null && !email.isBlank()) {
            auditLogService.logLogout(email);
        }
        return ResponseEntity.ok(new ApiMessageResponse("logout registrado"));
    }
}
