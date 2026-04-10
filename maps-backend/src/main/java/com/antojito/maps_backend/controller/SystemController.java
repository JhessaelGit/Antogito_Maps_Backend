package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.dto.ApiMessageResponse;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SystemController {

    @GetMapping("/")
    public ResponseEntity<ApiMessageResponse> root() {
        return ResponseEntity.ok(new ApiMessageResponse("Antojitos Maps Backend is running"));
    }

    @GetMapping({"/api/v1/health"})
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "UP");
        body.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(body);
    }
}
