package com.antojito.maps_backend.service;

import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

@Service
public class LogService {

    public void guardar(String email) {
        String log = "Email: " + email +
                     " | Fecha: " + LocalDateTime.now() + "\n";

        try (FileWriter writer = new FileWriter("registro.log", true)) {
            writer.write(log);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}