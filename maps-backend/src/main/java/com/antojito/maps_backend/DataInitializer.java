package com.antojito.maps_backend;

import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.RestaurantRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(RestaurantRepository repository) {
        return args -> {
            if (repository.count() == 0) { // Solo si la base está vacía
                Restaurante r1 = new Restaurante();
                r1.setNombre("Pollos Panchita");
                r1.setLat(-17.3895);
                r1.setLng(-66.1568);
                r1.setDescripcion("Pollo frito y combos familiares");
                repository.save(r1);

                Restaurante r2 = new Restaurante();
                r2.setNombre("Burger House");
                r2.setLat(-17.3950);
                r2.setLng(-66.1600);
                r2.setDescripcion("Hamburguesas artesanales");
                repository.save(r2);

                System.out.println("¡Datos de prueba insertados en Supabase!");
            }
        };
    }
}
