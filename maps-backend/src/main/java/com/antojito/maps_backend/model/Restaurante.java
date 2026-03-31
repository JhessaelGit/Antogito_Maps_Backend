package com.antojito.maps_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "restaurantes")
@Data 
public class Restaurante {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_restaurante")
    private Long id;

    private String nombre;
    private String correo;
    private String contraseña;
    
    // Columnas de tipo numeric/float
    private Double latitud;
    private Double longitud;
    
    // Datos de suscripción
    @Column(name = "plan_suscripcion")
    private String planSuscripcion;
    
    @Column(name = "fecha_vencimiento_plan")
    private LocalDate fechaVencimientoPlan;
    
    @Column(name = "estado_bloqueo")
    private Boolean estadoBloqueo;
    
    private String descripcion;
    
    @Column(name = "imagen_url")
    private String imagenUrl;

    // Coordenadas duplicadas (lat/lng) según tu DB
    private Double lat;
    private Double lng;
}