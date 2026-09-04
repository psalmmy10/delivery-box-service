package com.example.deliverybox.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI deliveryBoxOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Delivery Box Service API")
                        .description("REST API for managing delivery boxes and the items loaded onto them.")
                        .version("1.0.0")
                        .contact(new Contact().name("Delivery Box Team")));
    }
}