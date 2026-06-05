package me.dio.audioglow.domain.dto;

import java.util.List;

/**
 * JSON request payload expected by Google Gemini generateContent API.
 *
 * @param contents prompt contents grouped by role and text parts
 */
public record GeminiGenerateContentRequest(List<Content> contents) {

    public static GeminiGenerateContentRequest fromUserText(String text) {
        return new GeminiGenerateContentRequest(List.of(
                new Content("user", List.of(new Part(text)))
        ));
    }

    public record Content(String role, List<Part> parts) {
    }

    public record Part(String text) {
    }
}
