package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.AdminCreateRequest;
import com.antojito.maps_backend.dto.AdminLoginRequest;
import com.antojito.maps_backend.dto.AdminLoginResponse;
import com.antojito.maps_backend.dto.AdminResponse;
import com.antojito.maps_backend.dto.AdminRestaurantBlockRequest;
import com.antojito.maps_backend.dto.RestauranteResponse;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.model.Admin;
import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.AdminRepository;
import com.antojito.maps_backend.repository.RestauranteRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final RestauranteRepository restauranteRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public AdminLoginResponse login(String email) {
        String mail = normalizeMail(email);

        Admin admin = adminRepository.findByMailAndIsDeletedFalse(mail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "El administrador no esta registrado o esta inactivo"));

        auditLogService.logAdminLogin(mail);
        return new AdminLoginResponse(admin.getUuid(), admin.getMail(), "login correcto");
    }

    @Transactional
    public AdminResponse createAdmin(UUID actorAdminId, AdminCreateRequest request) {
        String mail = normalizeMail(request.getMail());
        long activeAdmins = adminRepository.countByIsDeletedFalse();

        if (activeAdmins > 0) {
            requireActiveAdmin(actorAdminId);
        }

        Admin existingByMail = adminRepository.findByMail(mail).orElse(null);
        if (existingByMail != null) {
            if (Boolean.FALSE.equals(existingByMail.getIsDeleted())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un admin con ese mail");
            }

            try {
                com.google.firebase.auth.UserRecord.UpdateRequest updateRequest = new com.google.firebase.auth.UserRecord.UpdateRequest(getFirebaseUidByEmail(mail))
                        .setPassword(request.getPassword())
                        .setDisabled(false);
                com.google.firebase.auth.FirebaseAuth.getInstance().updateUser(updateRequest);
            } catch (Exception e) {
                // If it doesn't exist in Firebase, create it
                createFirebaseUser(mail, request.getPassword());
            }

            existingByMail.setMail(mail);
            existingByMail.setPassword("FIREBASE_AUTH");
            existingByMail.setIsDeleted(Boolean.FALSE);
            existingByMail.setDeletedAt(null);
            Admin reactivated = adminRepository.save(existingByMail);
            auditLogService.logAdminCreate(actorAdminId, mail);
            return toAdminResponse(reactivated);
        }

        createFirebaseUser(mail, request.getPassword());

        Admin created = adminRepository.save(Admin.builder()
                .mail(mail)
                .password("FIREBASE_AUTH")
                .isDeleted(Boolean.FALSE)
                .deletedAt(null)
                .build());

        auditLogService.logAdminCreate(actorAdminId, mail);
        return toAdminResponse(created);
    }

    private void createFirebaseUser(String mail, String password) {
        try {
            com.google.firebase.auth.UserRecord.CreateRequest createRequest = new com.google.firebase.auth.UserRecord.CreateRequest()
                    .setEmail(mail)
                    .setPassword(password);
            com.google.firebase.auth.FirebaseAuth.getInstance().createUser(createRequest);
        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            if (!e.getAuthErrorCode().toString().equals("EMAIL_EXISTS")) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error creando admin en Firebase: " + e.getMessage());
            }
        }
    }

    private String getFirebaseUidByEmail(String mail) throws com.google.firebase.auth.FirebaseAuthException {
        return com.google.firebase.auth.FirebaseAuth.getInstance().getUserByEmail(mail).getUid();
    }

    @Transactional
    public AdminResponse updateOwnProfile(UUID actorAdminId, String mail, String password) {
        Admin actor = requireActiveAdmin(actorAdminId);
        String normalizedMail = normalizeMail(mail);

        if (adminRepository.existsByMailAndUuidNotAndIsDeletedFalse(normalizedMail, actor.getUuid())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un admin activo con ese mail");
        }

        try {
            com.google.firebase.auth.UserRecord.UpdateRequest updateRequest = new com.google.firebase.auth.UserRecord.UpdateRequest(getFirebaseUidByEmail(actor.getMail()))
                    .setEmail(normalizedMail)
                    .setPassword(password);
            com.google.firebase.auth.FirebaseAuth.getInstance().updateUser(updateRequest);
        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error actualizando admin en Firebase: " + e.getMessage());
        }

        actor.setMail(normalizedMail);
        actor.setPassword("FIREBASE_AUTH");

        Admin updated = adminRepository.save(actor);
        auditLogService.logAdminUpdate(updated.getMail());
        return toAdminResponse(updated);
    }

    @Transactional
    public void softDeleteAdmin(UUID actorAdminId, UUID targetAdminId) {
        Admin actor = requireActiveAdmin(actorAdminId);

        if (actor.getUuid().equals(targetAdminId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propio admin");
        }

        Admin target = adminRepository.findById(targetAdminId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe admin con uuid " + targetAdminId));

        if (Boolean.TRUE.equals(target.getIsDeleted())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El admin ya se encuentra eliminado");
        }

        target.setIsDeleted(Boolean.TRUE);
        target.setDeletedAt(LocalDateTime.now());
        adminRepository.save(target);

        auditLogService.logAdminDelete(actor.getMail(), target.getMail());
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> findActiveAdmins() {
        return adminRepository.findByIsDeletedFalseOrderByMailAsc().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> findDeletedAdmins() {
        return adminRepository.findByIsDeletedTrueOrderByDeletedAtDesc().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RestauranteResponse> findAllRestaurants(UUID actorAdminId) {
        requireActiveAdmin(actorAdminId);
        return restauranteRepository.findAll().stream()
                .map(this::toRestaurantResponse)
                .toList();
    }

    @Transactional
    public RestauranteResponse updateRestaurantBlockStatus(
            UUID actorAdminId,
            UUID restaurantId,
            AdminRestaurantBlockRequest request) {
        Admin actor = requireActiveAdmin(actorAdminId);

        Restaurante restaurante = restauranteRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe restaurante con uuid " + restaurantId));

        restaurante.setIsBlocked(request.getIsBlocked());
        Restaurante updated = restauranteRepository.save(restaurante);

        auditLogService.logAdminRestaurantBlock(actor.getMail(), updated.getUuid(), updated.getIsBlocked());
        return toRestaurantResponse(updated);
    }

    @Transactional(readOnly = true)
    public Admin requireActiveAdmin(UUID adminId) {
        if (adminId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Header X-Admin-Id requerido");
        }

        return adminRepository.findByUuidAndIsDeletedFalse(adminId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Admin no autenticado o eliminado"));
    }

    private String normalizeMail(String mail) {
        return mail == null ? null : mail.trim().toLowerCase();
    }

    private AdminResponse toAdminResponse(Admin admin) {
        return new AdminResponse(
                admin.getUuid(),
                admin.getMail(),
                admin.getIsDeleted(),
                admin.getDeletedAt());
    }

    private RestauranteResponse toRestaurantResponse(Restaurante entity) {
        return RestauranteResponse.builder()
                .uuid(entity.getUuid())
                .name(entity.getName())
                .description(entity.getDescription())
                .imagenUrl(entity.getImagenUrl())
                .planSuscription(entity.getPlanSuscription())
                .planExpirationDate(entity.getPlanExpirationDate())
                .isBlocked(entity.getIsBlocked())
                .latitude(entity.getLatitude())
                .longitude(entity.getLongitude())
                .category(entity.getCategory())
                .build();
    }
}
