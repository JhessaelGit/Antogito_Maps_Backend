package com.antojito.maps_backend;

import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.RestauranteRepository;
import com.antojito.maps_backend.service.R2StorageService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Slf4j
@RequiredArgsConstructor
@Profile("!test")
public class DataInitializer {

    private final RestauranteRepository restauranteRepository;
    private final R2StorageService r2StorageService;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            List<SeedRestaurant> seedData = List.of(
                    new SeedRestaurant(
                            "Sabor Valluno",
                            -17.3922,
                            -66.1561,
                            "PREMIUM",
                            LocalDate.now().plusMonths(6),
                            false,
                            "Comida tipica cochabambina con menu ejecutivo y delivery.",
                            "https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=1400&q=80",
                            "Comida Tipica",
                            "owner.sabor@antojitosmaps.com",
                            "OwnerSabor2026!",
                            "Promo Almuerzo",
                            "20% de descuento en menu ejecutivo",
                            new BigDecimal("20.00"),
                            LocalDate.now(),
                            LocalDate.now().plusDays(30),
                            true),
                    new SeedRestaurant(
                            "Pizzeria Don Forno",
                            -17.3975,
                            -66.1624,
                            "PRO",
                            LocalDate.now().plusMonths(4),
                            false,
                            "Pizzas artesanales al horno de piedra y promociones familiares.",
                            "https://images.unsplash.com/photo-1513104890138-7c749659a591?auto=format&fit=crop&w=1400&q=80",
                            "Pizzeria",
                            "owner.donforno@antojitosmaps.com",
                            "OwnerForno2026!",
                            "2x1 Martes",
                            "2x1 en pizzas familiares los martes",
                            new BigDecimal("50.00"),
                            LocalDate.now(),
                            LocalDate.now().plusDays(45),
                            true),
                    new SeedRestaurant(
                            "Sushi Andino",
                            -17.3858,
                            -66.1532,
                            "BASIC",
                            LocalDate.now().plusMonths(2),
                            false,
                            "Fusiones nikkei-andinas con ingredientes frescos y cartas tematicas.",
                            "https://images.unsplash.com/photo-1579584425555-c3ce17fd4351?auto=format&fit=crop&w=1400&q=80",
                            "Sushi",
                            "owner.sushi@antojitosmaps.com",
                            "OwnerSushi2026!",
                            "Happy Hour",
                            "15% de descuento de 16:00 a 18:00",
                            new BigDecimal("15.00"),
                            LocalDate.now(),
                            LocalDate.now().plusDays(20),
                            true));

            int restaurantsCreated = 0;
            int restaurantsUpdated = 0;
            int ownersUpserted = 0;
            int promotionsUpserted = 0;

            for (SeedRestaurant seed : seedData) {
                Restaurante restaurante = restauranteRepository.findByName(seed.name()).orElse(null);

                if (restaurante == null) {
                    String imageUrl = r2StorageService.ensureR2ImageUrl(null, seed.imageSourceUrl(), seed.name());
                    restaurante = restauranteRepository.save(Restaurante.builder()
                            .name(seed.name())
                            .latitude(seed.latitude())
                            .longitude(seed.longitude())
                            .planSuscription(seed.planSuscription())
                            .planExpirationDate(seed.planExpirationDate())
                            .isBlocked(seed.isBlocked())
                            .description(seed.description())
                            .imagenUrl(imageUrl)
                            .category(seed.category())
                            .build());
                    restaurantsCreated++;
                }

                String imageUrl = r2StorageService.ensureR2ImageUrl(restaurante.getImagenUrl(), seed.imageSourceUrl(), seed.name());
                boolean changed = false;

                if (!Objects.equals(restaurante.getName(), seed.name())) {
                    restaurante.setName(seed.name());
                    changed = true;
                }

                if (!Objects.equals(restaurante.getLatitude(), seed.latitude())) {
                    restaurante.setLatitude(seed.latitude());
                    changed = true;
                }
                if (!Objects.equals(restaurante.getLongitude(), seed.longitude())) {
                    restaurante.setLongitude(seed.longitude());
                    changed = true;
                }
                if (!Objects.equals(restaurante.getPlanSuscription(), seed.planSuscription())) {
                    restaurante.setPlanSuscription(seed.planSuscription());
                    changed = true;
                }
                if (!Objects.equals(restaurante.getPlanExpirationDate(), seed.planExpirationDate())) {
                    restaurante.setPlanExpirationDate(seed.planExpirationDate());
                    changed = true;
                }
                if (!Objects.equals(restaurante.getIsBlocked(), seed.isBlocked())) {
                    restaurante.setIsBlocked(seed.isBlocked());
                    changed = true;
                }
                if (!Objects.equals(restaurante.getDescription(), seed.description())) {
                    restaurante.setDescription(seed.description());
                    changed = true;
                }
                if (!Objects.equals(restaurante.getCategory(), seed.category())) {
                    restaurante.setCategory(seed.category());
                    changed = true;
                }
                if (!Objects.equals(restaurante.getImagenUrl(), imageUrl)) {
                    restaurante.setImagenUrl(imageUrl);
                    changed = true;
                }

                if (changed) {
                    restauranteRepository.save(restaurante);
                    restaurantsUpdated++;
                }

                upsertOwner(restaurante.getUuid(), seed.ownerMail(), seed.ownerPassword());
                ownersUpserted++;

                upsertPromotion(restaurante.getUuid(), seed);
                promotionsUpserted++;
            }

            upsertDefaultAdmin();

            log.info(
                    "Semilla completada. restaurantsCreated={}, restaurantsUpdated={}, ownersUpserted={}, promotionsUpserted={}",
                    restaurantsCreated,
                    restaurantsUpdated,
                    ownersUpserted,
                    promotionsUpserted);
        };
    }

    private void upsertDefaultAdmin() {
        String adminMail = "admin@antojitosmaps.com";
        UUID adminUuid = UUID.nameUUIDFromBytes(adminMail.getBytes(StandardCharsets.UTF_8));

        jdbcTemplate.update(
                """
                insert into admin (uuid, mail, password)
                values (?, ?, ?)
                on conflict (mail)
                do update set password = excluded.password
                """,
                adminUuid,
                adminMail,
                "Admin2026!");
    }

    private void upsertOwner(UUID restaurantUuid, String mail, String password) {
        jdbcTemplate.update(
                """
                insert into owner_restaurant (id_restaurant, mail, password)
                values (?, ?, ?)
                on conflict (id_restaurant, mail)
                do update set password = excluded.password
                """,
                restaurantUuid,
                mail,
                password);
    }

    private void upsertPromotion(UUID restaurantUuid, SeedRestaurant seed) {
        UUID promotionUuid = UUID.nameUUIDFromBytes(
                (restaurantUuid + "|" + seed.promotionTitle() + "|" + seed.dateStartPromotion())
                        .getBytes(StandardCharsets.UTF_8));

        jdbcTemplate.update(
                """
                insert into promotions (
                    uuid,
                    id_restaurant,
                    title,
                    description,
                    percent_discount,
                    date_start_promotion,
                    date_end_promotion,
                    is_active_promotion
                )
                values (?, ?, ?, ?, ?, ?, ?, ?)
                on conflict (uuid)
                do update set
                    description = excluded.description,
                    percent_discount = excluded.percent_discount,
                    date_start_promotion = excluded.date_start_promotion,
                    date_end_promotion = excluded.date_end_promotion,
                    is_active_promotion = excluded.is_active_promotion
                """,
                promotionUuid,
                restaurantUuid,
                seed.promotionTitle(),
                seed.promotionDescription(),
                seed.percentDiscount(),
                seed.dateStartPromotion(),
                seed.dateEndPromotion(),
                seed.isActivePromotion());
    }

    private record SeedRestaurant(
            String name,
            Double latitude,
            Double longitude,
            String planSuscription,
            LocalDate planExpirationDate,
            Boolean isBlocked,
            String description,
            String imageSourceUrl,
            String category,
            String ownerMail,
            String ownerPassword,
            String promotionTitle,
            String promotionDescription,
            BigDecimal percentDiscount,
            LocalDate dateStartPromotion,
            LocalDate dateEndPromotion,
            Boolean isActivePromotion) {
    }
}
