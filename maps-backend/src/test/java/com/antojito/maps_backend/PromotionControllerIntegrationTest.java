package com.antojito.maps_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.PromotionRepository;
import com.antojito.maps_backend.repository.RestauranteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromotionControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @Autowired
    private PromotionRepository promotionRepository;

    @BeforeEach
    void prepareDatabase() {
        jdbcTemplate.execute("""
                create table if not exists owner_account (
                    uuid uuid primary key,
                    mail varchar(150) not null unique,
                    password varchar(255) not null
                )
                """);

        jdbcTemplate.execute("""
                create table if not exists owner_restaurant (
                    id_owner uuid not null,
                    id_restaurant uuid not null,
                    primary key (id_owner, id_restaurant)
                )
                """);

        jdbcTemplate.update("delete from owner_restaurant");
        jdbcTemplate.update("delete from owner_account");
        promotionRepository.deleteAll();
        restauranteRepository.deleteAll();
    }

    @Test
    void createAndGetPromotionsByRestaurantShouldWork() throws Exception {
        Restaurante restaurante = restauranteRepository.save(Restaurante.builder()
                .name("Sabor Valluno")
                .description("Comida tipica")
                .imagenUrl("https://example.com/sabor-valluno.jpg")
                .planSuscription("PREMIUM")
                .planExpirationDate(LocalDate.now().plusDays(15))
                .isBlocked(Boolean.FALSE)
                .latitude(-17.3922)
                .longitude(-66.1561)
                .category("Tipica")
                .build());

        UUID ownerUuid = UUID.randomUUID();
        String ownerMail = "owner.sabor@antojitosmaps.com";

        jdbcTemplate.update(
                "insert into owner_account (uuid, mail, password) values (?, ?, ?)",
                ownerUuid,
                ownerMail,
                "Owner2026!");

        jdbcTemplate.update(
                "insert into owner_restaurant (id_owner, id_restaurant) values (?, ?)",
                ownerUuid,
                restaurante.getUuid());

        String createBody = """
                {
                                                                        "ownerUuid": "%s",
                  "title": "2x1 en saltenas",
                  "description": "Solo de lunes a viernes",
                  "percentDiscount": 25.0,
                  "dateStartPromotion": "2026-04-20",
                  "dateEndPromotion": "2026-04-30",
                  "isActivePromotion": true
                }
                                                                """.formatted(ownerUuid);

        MvcResult createResult = mockMvc.perform(post("/promotion/restaurant/{restaurantId}", restaurante.getUuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdPromotion = objectMapper.readTree(createResult.getResponse().getContentAsString());
        assertThat(createdPromotion.get("restaurantId").asText()).isEqualTo(restaurante.getUuid().toString());
        assertThat(createdPromotion.get("title").asText()).isEqualTo("2x1 en saltenas");
        assertThat(createdPromotion.get("percentDiscount").asDouble()).isEqualTo(25.0);
        assertThat(createdPromotion.get("isActivePromotion").asBoolean()).isTrue();

        MvcResult listResult = mockMvc.perform(get("/promotion/restaurant/{restaurantId}", restaurante.getUuid()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode promotions = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(promotions.isArray()).isTrue();
        assertThat(promotions.size()).isEqualTo(1);
        assertThat(promotions.get(0).get("title").asText()).isEqualTo("2x1 en saltenas");
    }

    @Test
    void createPromotionShouldFailWhenOwnerIsNotRelatedToRestaurant() throws Exception {
        Restaurante restaurante = restauranteRepository.save(Restaurante.builder()
                .name("Antojito 360")
                .description("Snacks")
                .imagenUrl("https://example.com/antojito-360.jpg")
                .planSuscription("BASIC")
                .planExpirationDate(LocalDate.now().plusDays(7))
                .isBlocked(Boolean.FALSE)
                .latitude(-17.4)
                .longitude(-66.1)
                .category("Rapida")
                .build());

        jdbcTemplate.update(
                "insert into owner_account (uuid, mail, password) values (?, ?, ?)",
                UUID.randomUUID(),
                "owner.sin.relacion@antojitosmaps.com",
                "Owner2026!");

        String createBody = """
                {
                  "ownerMail": "owner.sin.relacion@antojitosmaps.com",
                  "title": "Happy hour",
                  "description": "Promo sin relacion",
                  "percentDiscount": 15.0,
                  "dateStartPromotion": "2026-04-20",
                  "dateEndPromotion": "2026-04-30",
                  "isActivePromotion": true
                }
                """;

        mockMvc.perform(post("/promotion/restaurant/{restaurantId}", restaurante.getUuid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isForbidden());
    }
}
