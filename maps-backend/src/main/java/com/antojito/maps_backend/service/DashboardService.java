package com.antojito.maps_backend.service;

import com.antojito.maps_backend.dto.DashboardResponse;
import com.antojito.maps_backend.repository.LoyaltyAccountRepository;
import com.antojito.maps_backend.repository.PointsHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointsHistoryRepository pointsHistoryRepository;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long totalClientsWithPoints = loyaltyAccountRepository.countClientsWithPoints();
        long bronzeUsers = loyaltyAccountRepository.countByCurrentLevel("BRONCE");
        long silverUsers = loyaltyAccountRepository.countByCurrentLevel("PLATA");
        long goldUsers = loyaltyAccountRepository.countByCurrentLevel("ORO");
        long totalPointsDelivered = pointsHistoryRepository.sumAllPoints();

        return DashboardResponse.builder()
                .totalClientsWithPoints(totalClientsWithPoints)
                .bronzeUsers(bronzeUsers)
                .silverUsers(silverUsers)
                .goldUsers(goldUsers)
                .totalPointsDelivered(totalPointsDelivered)
                .build();
    }
}
