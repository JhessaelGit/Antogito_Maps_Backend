package com.antojito.maps_backend;

import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.RestauranteRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(RestauranteRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                List<Restaurante> seedData = List.of(
                        Restaurante.builder()
                                .nombre("Pollos Panchita")
                                .correo("pollos.panchita@antojitosmaps.com")
                                .contrasena("seed1234")
                                .lat(-17.3895)
                                .lng(-66.1568)
                                .latitud(-17.3895)
                                .longitud(-66.1568)
                                .descripcion("Pollo frito y combos familiares")
                                .build(),
                        Restaurante.builder()
                                .nombre("Burger House")
                                .correo("burger.house@antojitosmaps.com")
                                .contrasena("seed1234")
                                .lat(-17.3950)
                                .lng(-66.1600)
                                .latitud(-17.3950)
                                .longitud(-66.1600)
                                .descripcion("Hamburguesas artesanales")
                                .build());

                repository.saveAll(seedData);
                log.info("Datos de semilla insertados correctamente.");
            }
        };
    }
}
