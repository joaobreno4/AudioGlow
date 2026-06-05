package me.dio.audioglow.domain.dto;

/**
 * Response returned by OpenAI Whisper transcription endpoint.
 *
 * @param text transcribed text extracted from the audio file
 */
public record WhisperTranscriptionResponse(String text) {
}
