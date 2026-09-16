package com.bank.payment.orchestration.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentOrchestrationOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Payment Orchestration API")
                .description("Test-payment submission, workflow status queries, and partner-response simulation")
                .version("1.0.0"));
    }
}
