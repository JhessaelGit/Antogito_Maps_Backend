package com.antojito.maps_backend.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "restaurantes")
@Data // Genera getters y setters automáticamente
public class Restaurante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_restaurante")
    private Long id;

    private String nombre;
    private String descripcion;
    
    // Coordenadas para Leaflet
    private Double lat;
    private Double lng;
    
    // Podemos agregar la imagenUrl
    private String imagenUrl;
}