package me.dio.audioglow.infrastructure.client;

import me.dio.audioglow.domain.dto.GeminiGenerateContentRequest;
import me.dio.audioglow.domain.dto.GeminiGenerateContentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(
        name = "geminiClient",
        url = "${external-ai.gemini.base-url:https://generativelanguage.googleapis.com}"
)
public interface GeminiClient {

    @PostMapping(
            value = "/v1beta/models/{model}:generateContent",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    GeminiGenerateContentResponse generateContent(
            @RequestHeader("x-goog-api-key") String apiKey,
            @PathVariable("model") String model,
            @RequestBody GeminiGenerateContentRequest request
    );
}
