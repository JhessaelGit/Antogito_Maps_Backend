package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.service.LogService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/log")
@CrossOrigin(origins = "*")
public class LogController {

    private final LogService logService;

    public LogController(LogService logService) {
        this.logService = logService;
    }

    @PostMapping
    public String guardarLog(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        logService.guardar(email);
        return "ok";
    }
}