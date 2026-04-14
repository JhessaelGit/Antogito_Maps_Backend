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
    public AdminLoginResponse login(AdminLoginRequest request) {
        String mail = normalizeMail(request.getMail());

        Admin admin = adminRepository.findByMailAndIsDeletedFalse(mail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas"));

        if (!admin.getPassword().equals(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
        }

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

            existingByMail.setMail(mail);
            existingByMail.setPassword(request.getPassword());
            existingByMail.setIsDeleted(Boolean.FALSE);
            existingByMail.setDeletedAt(null);
            Admin reactivated = adminRepository.save(existingByMail);
            auditLogService.logAdminCreate(actorAdminId, mail);
            return toAdminResponse(reactivated);
        }

        Admin created = adminRepository.save(Admin.builder()
                .mail(mail)
                .password(request.getPassword())
                .isDeleted(Boolean.FALSE)
                .deletedAt(null)
                .build());

        auditLogService.logAdminCreate(actorAdminId, mail);
        return toAdminResponse(created);
    }

    @Transactional
    public AdminResponse updateOwnProfile(UUID actorAdminId, String mail, String password) {
        Admin actor = requireActiveAdmin(actorAdminId);
        String normalizedMail = normalizeMail(mail);

        if (adminRepository.existsByMailAndUuidNotAndIsDeletedFalse(normalizedMail, actor.getUuid())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un admin activo con ese mail");
        }

        actor.setMail(normalizedMail);
        actor.setPassword(password);

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
