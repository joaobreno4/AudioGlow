package me.dio.audioglow.domain.dto;

import java.util.List;
import java.util.Optional;

/**
 * Partial response mapping for Gemini generateContent, focused on the generated text.
 *
 * @param candidates possible model responses returned by Gemini
 */
public record GeminiGenerateContentResponse(List<Candidate> candidates) {

    public String firstText() {
        return Optional.ofNullable(candidates)
                .stream()
                .flatMap(List::stream)
                .findFirst()
                .map(Candidate::content)
                .map(Content::parts)
                .stream()
                .flatMap(List::stream)
                .findFirst()
                .map(Part::text)
                .orElse("");
    }

    public record Candidate(Content content) {
    }

    public record Content(List<Part> parts) {
    }

    public record Part(String text) {
    }
}
