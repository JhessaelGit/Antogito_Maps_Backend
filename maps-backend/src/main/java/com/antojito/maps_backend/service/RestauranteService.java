package com.antojito.maps_backend.service;

import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.RestaurantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RestauranteService {

    @Autowired
    private RestaurantRepository repository;

    // Obtener todos para pintar en el mapa
    public List<Restaurante> obtenerTodos() {
        return repository.findAll();
    }

    // Guardar un nuevo restaurante
    public Restaurante guardarRestaurante(Restaurante restaurante) {
        return repository.save(restaurante);
    }
}