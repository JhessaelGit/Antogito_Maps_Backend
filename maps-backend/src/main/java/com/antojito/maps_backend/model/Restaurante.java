package com.antojito.maps_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "restaurantes")
@Data // Genera getters y setters automáticamente
public class Restaurante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String descripcion;
    
    // Coordenadas para Leaflet
    private Double lat;
    private Double lng;
    
    // Podemos agregar la imagenUrl para que no sea siempre el logo de panchita
    private String imagenUrl;
}