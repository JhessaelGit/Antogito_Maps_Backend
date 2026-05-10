package com.antojito.maps_backend.dto;

import com.antojito.maps_backend.model.ComplaintStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ComplaintReviewRequest {

    @NotNull(message = "El estado de revision es requerido")
    private ComplaintStatus status;
}
