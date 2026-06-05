package me.dio.audioglow.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "external-ai")
public record ExternalAiProperties(
        OpenAi openai,
        Gemini gemini
) {

    public record OpenAi(
            String apiKey,
            String transcriptionModel
    ) {
        public String authorizationHeader() {
            return "Bearer %s".formatted(apiKey);
        }
    }

    public record Gemini(
            String apiKey,
            String model
    ) {
    }
}
