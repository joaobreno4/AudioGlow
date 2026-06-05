package me.dio.audioglow.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import me.dio.audioglow.application.usecase.VoiceAssistantUseCase;
import me.dio.audioglow.domain.dto.GeminiGenerateContentRequest;
import me.dio.audioglow.domain.dto.VoiceAssistantResponse;
import me.dio.audioglow.infrastructure.client.GeminiClient;
import me.dio.audioglow.infrastructure.client.WhisperClient;
import me.dio.audioglow.infrastructure.config.ExternalAiProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class VoiceAssistantServiceImpl implements VoiceAssistantUseCase {

    private static final String TOTAL_PROCESSING_TIMER = "audioglow.voice_assistant.processing.duration";
    private static final String EXTERNAL_API_TIMER = "audioglow.voice_assistant.external_api.duration";
    private static final String FAILURE_COUNTER = "audioglow.voice_assistant.requests.failed";

    private final WhisperClient whisperClient;
    private final GeminiClient geminiClient;
    private final ExternalAiProperties properties;
    private final MeterRegistry meterRegistry;
    private final Timer totalProcessingTimer;
    private final Counter failedRequestsCounter;

    public VoiceAssistantServiceImpl(
            WhisperClient whisperClient,
            GeminiClient geminiClient,
            ExternalAiProperties properties,
            MeterRegistry meterRegistry
    ) {
        this.whisperClient = whisperClient;
        this.geminiClient = geminiClient;
        this.properties = properties;
        this.meterRegistry = meterRegistry;
        this.totalProcessingTimer = Timer.builder(TOTAL_PROCESSING_TIMER)
                .description("Total duration of voice assistant command processing")
                .publishPercentileHistogram()
                .register(meterRegistry);
        this.failedRequestsCounter = Counter.builder(FAILURE_COUNTER)
                .description("Number of failed voice assistant command requests")
                .register(meterRegistry);
    }

    @Override
    public VoiceAssistantResponse processVoiceCommand(MultipartFile audioFile) {
        var totalStartNanos = System.nanoTime();
        var fileSizeBytes = safeFileSize(audioFile);

        log.info(
                "voice_command_processing_started file_name={} file_size_bytes={} content_type={}",
                safeFileName(audioFile),
                fileSizeBytes,
                safeContentType(audioFile)
        );

        try {
            validateAudioFile(audioFile);

            var openAi = properties.openai();
            var gemini = properties.gemini();

            var whisperStartNanos = System.nanoTime();
            var transcription = whisperClient.transcribe(
                    openAi.authorizationHeader(),
                    audioFile,
                    openAi.transcriptionModel()
            );
            var whisperDurationNanos = System.nanoTime() - whisperStartNanos;
            recordExternalApiDuration("whisper", whisperDurationNanos);

            var transcribedText = transcription.text();
            log.info(
                    "whisper_transcription_completed duration_ms={} transcribed_text_length={}",
                    nanosToMillis(whisperDurationNanos),
                    transcribedText == null ? 0 : transcribedText.length()
            );
            log.debug("whisper_transcription_payload text={}", transcribedText);

            var geminiRequest = GeminiGenerateContentRequest.fromUserText(transcribedText);
            var geminiStartNanos = System.nanoTime();
            var geminiResponse = geminiClient.generateContent(
                    gemini.apiKey(),
                    gemini.model(),
                    geminiRequest
            );
            var geminiDurationNanos = System.nanoTime() - geminiStartNanos;
            recordExternalApiDuration("gemini", geminiDurationNanos);

            var aiResponse = geminiResponse.firstText();
            log.info(
                    "gemini_generation_completed duration_ms={} response_text_length={}",
                    nanosToMillis(geminiDurationNanos),
                    aiResponse.length()
            );
            log.debug("gemini_response_payload text={}", aiResponse);

            return new VoiceAssistantResponse(
                    transcribedText,
                    aiResponse,
                    Instant.now()
            );
        } catch (RuntimeException exception) {
            failedRequestsCounter.increment();
            log.warn(
                    "voice_command_processing_failed file_size_bytes={} elapsed_ms={} error_type={} error_message={}",
                    fileSizeBytes,
                    nanosToMillis(System.nanoTime() - totalStartNanos),
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
            throw exception;
        } finally {
            var totalDurationNanos = System.nanoTime() - totalStartNanos;
            totalProcessingTimer.record(totalDurationNanos, TimeUnit.NANOSECONDS);

            log.info(
                    "voice_command_processing_finished file_size_bytes={} total_duration_ms={}",
                    fileSizeBytes,
                    nanosToMillis(totalDurationNanos)
            );
        }
    }

    private void validateAudioFile(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new IllegalArgumentException("Audio file must not be empty.");
        }

        if (!StringUtils.hasText(audioFile.getOriginalFilename())) {
            throw new IllegalArgumentException("Audio file must have a valid filename.");
        }
    }

    private void recordExternalApiDuration(String provider, long durationNanos) {
        Timer.builder(EXTERNAL_API_TIMER)
                .description("Duration of external AI API calls")
                .tag("provider", provider)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    private long nanosToMillis(long durationNanos) {
        return TimeUnit.NANOSECONDS.toMillis(durationNanos);
    }

    private long safeFileSize(MultipartFile audioFile) {
        return audioFile == null ? 0 : audioFile.getSize();
    }

    private String safeFileName(MultipartFile audioFile) {
        return audioFile == null ? null : audioFile.getOriginalFilename();
    }

    private String safeContentType(MultipartFile audioFile) {
        return audioFile == null ? null : audioFile.getContentType();
    }
}
