package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.ComplaintCreateRequest;
import com.antojito.maps_backend.dto.ComplaintResponse;
import com.antojito.maps_backend.dto.ComplaintReviewRequest;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.model.Client;
import com.antojito.maps_backend.model.Complaint;
import com.antojito.maps_backend.model.ComplaintStatus;
import com.antojito.maps_backend.model.ComplaintType;
import com.antojito.maps_backend.model.Promotion;
import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.ClientRepository;
import com.antojito.maps_backend.repository.ComplaintRepository;
import com.antojito.maps_backend.repository.PromotionRepository;
import com.antojito.maps_backend.repository.RestauranteRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final RestauranteRepository restauranteRepository;
    private final PromotionRepository promotionRepository;
    private final ClientRepository clientRepository;
    private final AdminService adminService;
    private final EmailService emailService;

    @Transactional
    public ComplaintResponse createComplaint(UUID clientUuid, ComplaintCreateRequest request) {
        if (request.getType() == ComplaintType.RESTAURANT) {
            if (!restauranteRepository.existsById(request.getTargetUuid())) {
                throw new ResourceNotFoundException("Restaurante objetivo no encontrado");
            }
        } else if (request.getType() == ComplaintType.PROMOTION) {
            if (!promotionRepository.existsById(request.getTargetUuid())) {
                throw new ResourceNotFoundException("Promocion objetivo no encontrada");
            }
        }

        Complaint complaint = Complaint.builder()
                .clientUuid(clientUuid)
                .type(request.getType())
                .targetUuid(request.getTargetUuid())
                .description(request.getDescription())
                .status(ComplaintStatus.PENDING)
                .build();

        Complaint saved = complaintRepository.save(complaint);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getAllComplaints(UUID actorAdminId) {
        adminService.requireActiveAdmin(actorAdminId);
        return complaintRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> getPendingComplaints(UUID actorAdminId) {
        adminService.requireActiveAdmin(actorAdminId);
        return complaintRepository.findByStatusOrderByCreatedAtDesc(ComplaintStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ComplaintResponse reviewComplaint(UUID actorAdminId, UUID complaintId, ComplaintReviewRequest request) {
        adminService.requireActiveAdmin(actorAdminId);

        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ResourceNotFoundException("Queja no encontrada"));

        if (complaint.getStatus() != ComplaintStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Esta queja ya ha sido procesada");
        }

        if (request.getStatus() == ComplaintStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El estado debe ser ACCEPTED o REJECTED");
        }

        complaint.setStatus(request.getStatus());

        Restaurante restaurante = null;

        if (request.getStatus() == ComplaintStatus.ACCEPTED) {
            if (complaint.getType() == ComplaintType.RESTAURANT) {
                restaurante = restauranteRepository.findById(complaint.getTargetUuid()).orElse(null);
                if (restaurante != null) {
                    restaurante.setIsBlocked(true);
                    restauranteRepository.save(restaurante);
                }
            } else if (complaint.getType() == ComplaintType.PROMOTION) {
                Promotion promotion = promotionRepository.findById(complaint.getTargetUuid()).orElse(null);
                if (promotion != null) {
                    promotion.setIsActivePromotion(false);
                    promotionRepository.save(promotion);
                    // Obtener restaurante de la promoción para el email
                    restaurante = restauranteRepository.findById(promotion.getRestaurantId()).orElse(null);
                }
            }
        } else {
            // REJECTED — igual buscamos el restaurante para el email
            if (complaint.getType() == ComplaintType.RESTAURANT) {
                restaurante = restauranteRepository.findById(complaint.getTargetUuid()).orElse(null);
            } else if (complaint.getType() == ComplaintType.PROMOTION) {
                Promotion promotion = promotionRepository.findById(complaint.getTargetUuid()).orElse(null);
                if (promotion != null) {
                    restaurante = restauranteRepository.findById(promotion.getRestaurantId()).orElse(null);
                }
            }
        }

        Complaint updated = complaintRepository.save(complaint);

        // ── Notificación por email (async, no bloquea la respuesta) ──
        try {
            Client client = clientRepository.findById(complaint.getClientUuid()).orElse(null);
            if (client != null) {
                emailService.sendComplaintResolutionEmail(
                        client.getMail(),
                        client.getFullName(),
                        updated,
                        restaurante
                );
            }
        } catch (Exception e) {
            log.warn("[ComplaintService] No se pudo iniciar el envío de email: {}", e.getMessage());
        }

        return toResponse(updated);
    }

    // ── Mapper ──────────────────────────────────────────────────────────────

    private ComplaintResponse toResponse(Complaint entity) {
        String targetName     = null;
        String targetCategory = null;

        if (entity.getType() == ComplaintType.RESTAURANT) {
            Restaurante r = restauranteRepository.findById(entity.getTargetUuid()).orElse(null);
            if (r != null) {
                targetName     = r.getName();
                targetCategory = r.getCategory();
            }
        } else if (entity.getType() == ComplaintType.PROMOTION) {
            Promotion p = promotionRepository.findById(entity.getTargetUuid()).orElse(null);
            if (p != null) {
                targetName     = p.getTitle();
                targetCategory = "Promoción";
            }
        }

        return ComplaintResponse.builder()
                .uuid(entity.getUuid())
                .type(entity.getType())
                .targetUuid(entity.getTargetUuid())
                .targetName(targetName)
                .targetCategory(targetCategory)
                .description(entity.getDescription())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
