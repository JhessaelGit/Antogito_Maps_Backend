package com.antojito.maps_backend;

import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.RestauranteRepository;
import com.antojito.maps_backend.service.CloudflareImagesService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DataInitializer {

    private final CloudflareImagesService cloudflareImagesService;

    @Bean
    CommandLineRunner initDatabase(RestauranteRepository repository) {
        return args -> {
        List<SeedRestaurant> seedData = List.of(
            new SeedRestaurant(
                "Sabor Valluno",
                "sabor.valluno@antojitosmaps.com",
                "Sabor2026!",
                -17.3922,
                -66.1561,
                "Comida tipica cochabambina con menu ejecutivo y delivery.",
                "https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=1400&q=80",
                "PREMIUM",
                LocalDate.now().plusMonths(6),
                false),
            new SeedRestaurant(
                "Pizzeria Don Forno",
                "donforno@antojitosmaps.com",
                "Forno2026!",
                -17.3975,
                -66.1624,
                "Pizzas artesanales al horno de piedra y promociones familiares.",
                "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=1400&q=80",
                "PRO",
                LocalDate.now().plusMonths(4),
                false),
            new SeedRestaurant(
                "Sushi Andino",
                "sushi.andino@antojitosmaps.com",
                "Andino2026!",
                -17.3858,
                -66.1532,
                "Fusiones nikkei-andinas con ingredientes frescos y cartas tematicas.",
                "https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?auto=format&fit=crop&w=1400&q=80",
                "BASIC",
                LocalDate.now().plusMonths(2),
                false));

        int created = 0;
        int updatedImages = 0;

        for (SeedRestaurant seed : seedData) {
        Restaurante existing = repository.findByCorreo(seed.correo()).orElse(null);

        if (existing == null) {
            String imageUrl = cloudflareImagesService.ensureCloudflareImageUrl(
                null, seed.imageSourceUrl(), seed.nombre());

            Restaurante restaurant = Restaurante.builder()
                .nombre(seed.nombre())
                .correo(seed.correo())
                .contrasena(seed.contrasena())
                .lat(seed.lat())
                .lng(seed.lng())
                .latitud(seed.lat())
                .longitud(seed.lng())
                .descripcion(seed.descripcion())
                .imagenUrl(imageUrl)
                .planSuscripcion(seed.planSuscripcion())
                .fechaVencimientoPlan(seed.fechaVencimientoPlan())
                .estadoBloqueo(seed.estadoBloqueo())
                .build();

            repository.save(restaurant);
            created++;
            continue;
        }

        String upgradedImageUrl = cloudflareImagesService.ensureCloudflareImageUrl(
            existing.getImagenUrl(), seed.imageSourceUrl(), seed.nombre());

        if (upgradedImageUrl != null && !upgradedImageUrl.equals(existing.getImagenUrl())) {
            existing.setImagenUrl(upgradedImageUrl);
            repository.save(existing);
            updatedImages++;
        }
            }

        log.info("Semilla de restaurantes completada. creados={}, imagenesActualizadas={}", created, updatedImages);
        };
    }

    private record SeedRestaurant(
        String nombre,
        String correo,
        String contrasena,
        Double lat,
        Double lng,
        String descripcion,
        String imageSourceUrl,
        String planSuscripcion,
        LocalDate fechaVencimientoPlan,
        Boolean estadoBloqueo) {
    }
}
