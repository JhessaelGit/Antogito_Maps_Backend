package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import com.antojito.maps_backend.dto.ClientLoginRequest;
import com.antojito.maps_backend.dto.ClientLoginResponse;
import com.antojito.maps_backend.dto.ClientRegistryRequest;
import com.antojito.maps_backend.model.Client;
import com.antojito.maps_backend.repository.ClientRepository;
import com.antojito.maps_backend.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/client")
@CrossOrigin(originPatterns = "${app.cors.allowed-origins:*}")
@RequiredArgsConstructor
@Tag(name = "Client Auth", description = "Autenticacion de clientes (Firebase gestionado por el backend)")
public class ClientController {

    private final ClientRepository clientRepository;
    private final AuditLogService auditLogService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${firebase.api-key}")
    private String firebaseApiKey;

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // REGISTRO: crea usuario en Firebase + guarda en BD
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        createFirebaseUser(email, request.getPassword());

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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // LOGIN: autentica en Firebase REST + devuelve datos del cliente
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // LOGOUT
    // â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PostMapping("/logout")
    @Operation(summary = "Logout de cliente", description = "Registra el cierre de sesion del cliente")
    public ResponseEntity<ApiMessageResponse> logout(@RequestBody Map<String, String> payload) {
        String email = payload.get("mail");
        if (email != null && !email.isBlank()) {
            auditLogService.logLogout(email);
        }
        return ResponseEntity.ok(new ApiMessageResponse("logout registrado"));
    }

    private void createFirebaseUser(String email, String password) {
        String firebaseUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=" + firebaseApiKey;
        try {
            Map<String, Object> body = Map.of(
                    "email", email,
                    "password", password,
                    "returnSecureToken", true);
            restTemplate.postForEntity(firebaseUrl, body, Map.class);
        } catch (HttpClientErrorException exception) {
            String responseBody = exception.getResponseBodyAsString();
            if (responseBody.contains("EMAIL_EXISTS")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo ya esta registrado en Firebase");
            }
            if (responseBody.contains("WEAK_PASSWORD")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contrasena debe tener al menos 6 caracteres");
            }
            if (responseBody.contains("INVALID_EMAIL")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo no tiene formato valido");
            }
            if (responseBody.contains("OPERATION_NOT_ALLOWED")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El registro con email y contrasena no esta habilitado en Firebase");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo crear el usuario en Firebase");
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo conectar con Firebase");
        }
    }
}

