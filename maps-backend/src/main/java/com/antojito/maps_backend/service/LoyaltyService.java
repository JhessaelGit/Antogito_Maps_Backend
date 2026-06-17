package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.LoyaltyResponse;
import com.antojito.maps_backend.exception.ResourceNotFoundException;
import com.antojito.maps_backend.model.Client;
import com.antojito.maps_backend.model.LoyaltyAccount;
import com.antojito.maps_backend.model.PointsHistory;
import com.antojito.maps_backend.repository.ClientRepository;
import com.antojito.maps_backend.repository.LoyaltyAccountRepository;
import com.antojito.maps_backend.repository.PointsHistoryRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
public class LoyaltyService {

    private final ClientRepository clientRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointsHistoryRepository pointsHistoryRepository;

    @Transactional
    public LoyaltyResponse addPoints(UUID clientId, Integer points, String reason) {
        if (points == null || points <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los puntos deben ser un valor positivo");
        }
        if (reason == null || reason.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La razon es requerida");
        }

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe cliente con uuid " + clientId));

        LoyaltyAccount loyaltyAccount = loyaltyAccountRepository.findByClient_Uuid(clientId)
                .orElseGet(() -> createLoyaltyAccount(client));

        loyaltyAccount.setAccumulatedPoints(loyaltyAccount.getAccumulatedPoints() + points);
        loyaltyAccount.setCurrentLevel(calculateLevel(loyaltyAccount.getAccumulatedPoints()));
        loyaltyAccount = loyaltyAccountRepository.save(loyaltyAccount);

        pointsHistoryRepository.save(
                PointsHistory.builder()
                        .client(client)
                        .points(points)
                        .reason(reason.trim())
                        .build());

        return toResponse(loyaltyAccount);
    }

    @Transactional
    public LoyaltyResponse getProfile(UUID clientId) {
        LoyaltyAccount loyaltyAccount = loyaltyAccountRepository.findByClient_Uuid(clientId)
                .orElseGet(() -> loyaltyAccountRepository.save(createLoyaltyAccount(getClient(clientId))));
        return toResponse(loyaltyAccount);
    }

    public String calculateLevel(Integer points) {
        if (points == null) {
            return "BRONCE";
        }
        if (points < 100) {
            return "BRONCE";
        }
        if (points < 300) {
            return "PLATA";
        }
        return "ORO";
    }

    private LoyaltyAccount createLoyaltyAccount(Client client) {
        return LoyaltyAccount.builder()
                .client(client)
                .accumulatedPoints(0)
                .currentLevel("BRONCE")
                .build();
    }

    private Client getClient(UUID clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("No existe cliente con uuid " + clientId));
    }

    private LoyaltyResponse toResponse(LoyaltyAccount loyaltyAccount) {
        return LoyaltyResponse.builder()
                .clientId(loyaltyAccount.getClient().getUuid())
                .points(loyaltyAccount.getAccumulatedPoints())
                .level(loyaltyAccount.getCurrentLevel())
                .build();
    }
}
