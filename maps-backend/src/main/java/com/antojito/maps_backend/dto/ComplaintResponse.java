package com.antojito.maps_backend.dto;

import com.antojito.maps_backend.model.ComplaintStatus;
import com.antojito.maps_backend.model.ComplaintType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintResponse {
    private UUID uuid;
    private ComplaintType type;
    private UUID targetUuid;
    private String description;
    private ComplaintStatus status;
    private LocalDateTime createdAt;
}
