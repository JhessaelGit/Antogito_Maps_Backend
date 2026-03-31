package com.antojito.maps_backend.controller;

import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.service.RestauranteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/restaurantes")
@CrossOrigin(origins = "*") // Para que Angular pueda hacer peticiones
public class RestauranteController {

    @Autowired
    private RestauranteService service;

    // GET: http://localhost:8080/api/restaurantes (Lo usará tu frontend)
    @GetMapping
    public List<Restaurante> listarRestaurantes() {
        return service.obtenerTodos();
    }

    // POST: http://localhost:8080/api/restaurantes (Para agregar nuevos)
    @PostMapping
    public Restaurante crearRestaurante(@RequestBody Restaurante restaurante) {
        return service.guardarRestaurante(restaurante);
    }
}