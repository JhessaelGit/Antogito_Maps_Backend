package com.antojito.maps_backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.antojito.maps_backend.model.Restaurante;
import com.antojito.maps_backend.repository.AdminRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RestauranteRepository restauranteRepository;

    @BeforeEach
    void cleanDatabase() {
        adminRepository.deleteAll();
        restauranteRepository.deleteAll();
    }

    @Test
    void bootstrapCreateAndLoginShouldWork() throws Exception {
        String createBody = """
                {
                  "mail": "BOOTSTRAP.ADMIN@antojitosmaps.com",
                  "password": "Bootstrap2026!"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/admin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        UUID adminId = UUID.fromString(created.get("uuid").asText());
        assertThat(created.get("mail").asText()).isEqualTo("bootstrap.admin@antojitosmaps.com");
        assertThat(created.get("isDeleted").asBoolean()).isFalse();

        String loginBody = """
                {
                  "mail": "bootstrap.admin@antojitosmaps.com",
                  "password": "Bootstrap2026!"
                }
                """;

        MvcResult loginResult = mockMvc.perform(post("/admin/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode login = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        assertThat(UUID.fromString(login.get("adminId").asText())).isEqualTo(adminId);
    }

    @Test
    void fullAdminCrudFlowShouldWork() throws Exception {
        UUID actorAdminId = bootstrapAdmin("actor.admin@antojitosmaps.com", "Actor2026!");

        String createSecondBody = """
                {
                  "mail": "second.admin@antojitosmaps.com",
                  "password": "Second2026!"
                }
                """;

        MvcResult createSecondResult = mockMvc.perform(post("/admin/create")
                        .header("X-Admin-Id", actorAdminId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createSecondBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode secondAdmin = objectMapper.readTree(createSecondResult.getResponse().getContentAsString());
        UUID secondAdminId = UUID.fromString(secondAdmin.get("uuid").asText());

        String editSecondBody = """
                {
                  "mail": "second.admin.updated@antojitosmaps.com",
                  "password": "Second2027!"
                }
                """;

        mockMvc.perform(put("/admin/edit")
                        .header("X-Admin-Id", secondAdminId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(editSecondBody))
                .andExpect(status().isOk());

        MvcResult listActiveResult = mockMvc.perform(get("/admin/all"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode activeAdmins = objectMapper.readTree(listActiveResult.getResponse().getContentAsString());
        assertThat(activeAdmins.isArray()).isTrue();
        assertThat(activeAdmins.size()).isEqualTo(2);

        mockMvc.perform(delete("/admin/delete/{id}", secondAdminId)
                        .header("X-Admin-Id", actorAdminId.toString()))
                .andExpect(status().isOk());

        MvcResult deletedResult = mockMvc.perform(get("/admin/deleted"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode deletedAdmins = objectMapper.readTree(deletedResult.getResponse().getContentAsString());
        assertThat(deletedAdmins.isArray()).isTrue();
        assertThat(deletedAdmins.size()).isEqualTo(1);
        assertThat(UUID.fromString(deletedAdmins.get(0).get("uuid").asText())).isEqualTo(secondAdminId);

        mockMvc.perform(delete("/admin/delete/{id}", actorAdminId)
                        .header("X-Admin-Id", actorAdminId.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void restaurantModerationEndpointsShouldWork() throws Exception {
        UUID actorAdminId = bootstrapAdmin("mod.admin@antojitosmaps.com", "Mod2026!");

        Restaurante restaurante = restauranteRepository.save(Restaurante.builder()
                .name("Sabor Andino")
                .description("Comida tradicional")
                .imagenUrl("https://example.com/sabor-andino.jpg")
                .planSuscription("FREE")
                .planExpirationDate(LocalDate.now().plusDays(30))
                .isBlocked(Boolean.FALSE)
                .latitude(-17.3922)
                .longitude(-66.1561)
                .category("Tipica")
                .build());

        String blockBody = """
                {
                  "isBlocked": true
                }
                """;

        MvcResult blockResult = mockMvc.perform(patch("/admin/restaurants/{id}/block", restaurante.getUuid())
                        .header("X-Admin-Id", actorAdminId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(blockBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode blockedRestaurant = objectMapper.readTree(blockResult.getResponse().getContentAsString());
        assertThat(blockedRestaurant.get("isBlocked").asBoolean()).isTrue();

        MvcResult listRestaurantsResult = mockMvc.perform(get("/admin/restaurants")
                        .header("X-Admin-Id", actorAdminId.toString()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode restaurants = objectMapper.readTree(listRestaurantsResult.getResponse().getContentAsString());
        assertThat(restaurants.isArray()).isTrue();
        assertThat(restaurants.size()).isEqualTo(1);
        assertThat(restaurants.get(0).get("isBlocked").asBoolean()).isTrue();
    }

    private UUID bootstrapAdmin(String mail, String password) throws Exception {
        String createBody = """
                {
                  "mail": "%s",
                  "password": "%s"
                }
                """.formatted(mail, password);

        MvcResult result = mockMvc.perform(post("/admin/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(created.get("uuid").asText());
    }
}
