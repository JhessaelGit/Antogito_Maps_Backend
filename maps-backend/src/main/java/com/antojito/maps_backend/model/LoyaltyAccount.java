package com.antojito.maps_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "loyalty_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID uuid;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_uuid", nullable = false, unique = true)
    private Client client;

    @Column(name = "accumulated_points", nullable = false)
    private Integer accumulatedPoints;

    @Column(name = "current_level", nullable = false, length = 50)
    private String currentLevel;
}
