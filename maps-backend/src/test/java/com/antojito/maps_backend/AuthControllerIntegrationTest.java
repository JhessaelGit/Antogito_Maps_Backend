package com.antojito.maps_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.antojito.maps_backend.model.Restaurante;
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
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestauranteRepository restauranteRepository;

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
        restauranteRepository.deleteAll();
    }

    @Test
    void loginShouldReturnOwnerAndRestaurants() throws Exception {
        Restaurante restaurantOne = restauranteRepository.save(Restaurante.builder()
                .name("Antojito One")
                .description("Descripcion 1")
                .imagenUrl("https://example.com/one.jpg")
                .planSuscription("BASIC")
                .planExpirationDate(LocalDate.now().plusDays(5))
                .isBlocked(Boolean.FALSE)
                .latitude(-17.39)
                .longitude(-66.15)
                .category("Tipica")
                .build());

        Restaurante restaurantTwo = restauranteRepository.save(Restaurante.builder()
                .name("Antojito Two")
                .description("Descripcion 2")
                .imagenUrl("https://example.com/two.jpg")
                .planSuscription("PREMIUM")
                .planExpirationDate(LocalDate.now().plusDays(10))
                .isBlocked(Boolean.FALSE)
                .latitude(-17.40)
                .longitude(-66.14)
                .category("Rapida")
                .build());

        UUID ownerUuid = UUID.randomUUID();
        String ownerMail = "owner.login@antojitosmaps.com";
        String ownerPassword = "OwnerLogin2026!";

        jdbcTemplate.update(
                "insert into owner_account (uuid, mail, password) values (?, ?, ?)",
                ownerUuid,
                ownerMail,
                ownerPassword);

        jdbcTemplate.update(
                "insert into owner_restaurant (id_owner, id_restaurant) values (?, ?)",
                ownerUuid,
                restaurantOne.getUuid());
        jdbcTemplate.update(
                "insert into owner_restaurant (id_owner, id_restaurant) values (?, ?)",
                ownerUuid,
                restaurantTwo.getUuid());

        String requestBody = """
                {
                  "mail": "owner.login@antojitosmaps.com",
                  "password": "OwnerLogin2026!"
                }
                """;

        MvcResult result = mockMvc.perform(post("/restaurant/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(response.get("ownerId").asText()).isEqualTo(ownerUuid.toString());
        assertThat(response.get("mail").asText()).isEqualTo(ownerMail);
        assertThat(response.get("message").asText()).isEqualTo("login correcto");
        assertThat(response.get("restaurantIds").isArray()).isTrue();
        assertThat(response.get("restaurantIds").size()).isEqualTo(2);
    }
}
