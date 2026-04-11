package com.antojito.maps_backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI antogitoMapsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Antojitos Maps Backend API")
                        .description("Documentacion de endpoints para autenticacion, restaurantes y salud del sistema.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Equipo Antojitos Maps")
                                .email("soporte@antojitosmaps.com"))
                        .license(new License()
                                .name("Uso interno")
                                .url("https://antojitosmaps.com")));
    }
}
