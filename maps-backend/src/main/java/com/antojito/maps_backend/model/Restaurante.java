package com.antojito.maps_backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurantes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Restaurante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_restaurante")
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String correo;

    @Column(name = "contraseña", nullable = false, length = 255)
    private String contrasena;

    private Double latitud;
    private Double longitud;

    @Column(name = "plan_suscripcion", length = 60)
    private String planSuscripcion;

    @Column(name = "fecha_vencimiento_plan")
    private LocalDate fechaVencimientoPlan;

    @Column(name = "estado_bloqueo")
    private Boolean estadoBloqueo;

    @Column(length = 500)
    private String descripcion;

    @Column(name = "imagen_url", length = 500)
    private String imagenUrl;

    private Double lat;
    private Double lng;

    @PrePersist
    public void applyDefaults() {
        if (estadoBloqueo == null) {
            estadoBloqueo = Boolean.FALSE;
        }
    }
}