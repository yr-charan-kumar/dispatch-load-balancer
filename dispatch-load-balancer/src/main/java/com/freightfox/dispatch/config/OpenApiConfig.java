package com.freightfox.dispatch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI dispatchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Dispatch Load Balancer API")
                .version("1.0.0")
                .description("""
                        Assigns delivery orders to a fleet while respecting vehicle capacity,
                        serving HIGH priority orders first and minimising travel distance
                        (Haversine). Typical flow: POST vehicles -> POST orders -> GET plan.
                        """));
    }
}
