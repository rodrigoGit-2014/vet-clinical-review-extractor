package com.vetplatform.reviewextractor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reviewExtractorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clinical Review Extractor & Vet Recommendation API")
                        .description("""
                                API del componente de extraccion y estructuracion de resenas clinicas veterinarias,
                                y recomendacion de veterinarios basada en casos clinicos similares.

                                Este componente recibe resenas en texto libre escritas por duenos de mascotas,
                                las procesa mediante un LLM y genera datos clinicos estructurados y normalizados.

                                **Modulo 1 — Extraccion de resenas clinicas:**
                                1. `POST /clinical-reviews` — Enviar resena para procesamiento (asincrono)
                                2. `GET /clinical-reviews/{id}/status` — Consultar estado del procesamiento
                                3. `GET /clinical-reviews/{id}/result` — Obtener resultado estructurado
                                4. `POST /clinical-reviews/{id}/reprocess` — Reprocesar una resena

                                **Modulo 2 — Recomendacion de veterinarios:**
                                1. `POST /vet-recommendations` — Enviar solicitud de recomendacion (asincrono)
                                2. `GET /vet-recommendations/{id}/status` — Consultar estado del procesamiento
                                3. `GET /vet-recommendations/{id}/result` — Obtener ranking de veterinarios recomendados
                                4. `POST /vet-recommendations/{id}/recalculate` — Recalcular una recomendacion
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("VetPlatform Team")
                                .email("dev@vetplatform.com")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development")
                ));
    }
}
