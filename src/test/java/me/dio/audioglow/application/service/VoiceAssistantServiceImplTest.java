package me.dio.audioglow.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import me.dio.audioglow.domain.dto.GeminiGenerateContentRequest;
import me.dio.audioglow.domain.dto.GeminiGenerateContentResponse;
import me.dio.audioglow.domain.dto.WhisperTranscriptionResponse;
import me.dio.audioglow.infrastructure.client.GeminiClient;
import me.dio.audioglow.infrastructure.client.WhisperClient;
import me.dio.audioglow.infrastructure.config.ExternalAiProperties;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class VoiceAssistantServiceImplTest {

    private static final String OPENAI_API_KEY = "openai-test-key";
    private static final String OPENAI_AUTHORIZATION = "Bearer " + OPENAI_API_KEY;
    private static final String WHISPER_MODEL = "whisper-1";
    private static final String GEMINI_API_KEY = "gemini-test-key";
    private static final String GEMINI_MODEL = "gemini-1.5-flash";

    @Mock
    private WhisperClient whisperClient;

    @Mock
    private GeminiClient geminiClient;

    private SimpleMeterRegistry meterRegistry;

    private VoiceAssistantServiceImpl service;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        var properties = new ExternalAiProperties(
                new ExternalAiProperties.OpenAi(OPENAI_API_KEY, WHISPER_MODEL),
                new ExternalAiProperties.Gemini(GEMINI_API_KEY, GEMINI_MODEL)
        );

        service = new VoiceAssistantServiceImpl(whisperClient, geminiClient, properties, meterRegistry);
    }

    @Test
    void shouldProcessVoiceCommandSuccessfully() {
        var audioFile = audioFile();
        var transcribedText = "Acenda as luzes da sala";
        var aiAnswer = "As luzes da sala foram acesas.";
        var geminiResponse = new GeminiGenerateContentResponse(List.of(
                new GeminiGenerateContentResponse.Candidate(
                        new GeminiGenerateContentResponse.Content(List.of(
                                new GeminiGenerateContentResponse.Part(aiAnswer)
                        ))
                )
        ));

        when(whisperClient.transcribe(OPENAI_AUTHORIZATION, audioFile, WHISPER_MODEL))
                .thenReturn(new WhisperTranscriptionResponse(transcribedText));
        when(geminiClient.generateContent(eq(GEMINI_API_KEY), eq(GEMINI_MODEL), any(GeminiGenerateContentRequest.class)))
                .thenReturn(geminiResponse);

        var response = service.processVoiceCommand(audioFile);

        assertThat(response.textoTranscrito()).isEqualTo(transcribedText);
        assertThat(response.respostaIA()).isEqualTo(aiAnswer);
        assertThat(response.timestamp()).isNotNull();
        assertThat(meterRegistry.get("audioglow.voice_assistant.processing.duration").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("audioglow.voice_assistant.external_api.duration").tag("provider", "whisper").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("audioglow.voice_assistant.external_api.duration").tag("provider", "gemini").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("audioglow.voice_assistant.requests.failed").counter().count()).isZero();

        verify(whisperClient).transcribe(OPENAI_AUTHORIZATION, audioFile, WHISPER_MODEL);

        var requestCaptor = ArgumentCaptor.forClass(GeminiGenerateContentRequest.class);
        verify(geminiClient).generateContent(eq(GEMINI_API_KEY), eq(GEMINI_MODEL), requestCaptor.capture());

        var geminiRequest = requestCaptor.getValue();
        assertThat(geminiRequest.contents()).hasSize(1);
        assertThat(geminiRequest.contents().get(0).role()).isEqualTo("user");
        assertThat(geminiRequest.contents().get(0).parts()).hasSize(1);
        assertThat(geminiRequest.contents().get(0).parts().get(0).text()).isEqualTo(transcribedText);
    }

    @Test
    void shouldPropagateFeignExceptionWhenWhisperFails() {
        var audioFile = audioFile();
        var exception = feignException();

        when(whisperClient.transcribe(OPENAI_AUTHORIZATION, audioFile, WHISPER_MODEL))
                .thenThrow(exception);

        assertThatThrownBy(() -> service.processVoiceCommand(audioFile))
                .isSameAs(exception);
        assertThat(meterRegistry.get("audioglow.voice_assistant.processing.duration").timer().count()).isEqualTo(1);
        assertThat(meterRegistry.get("audioglow.voice_assistant.requests.failed").counter().count()).isEqualTo(1);

        verify(whisperClient).transcribe(OPENAI_AUTHORIZATION, audioFile, WHISPER_MODEL);
        verify(geminiClient, never()).generateContent(any(), any(), any());
    }

    private MultipartFile audioFile() {
        return new MockMultipartFile(
                "audioFile",
                "command.wav",
                "audio/wav",
                "fake-audio-content".getBytes(StandardCharsets.UTF_8)
        );
    }

    private FeignException feignException() {
        var request = Request.create(
                Request.HttpMethod.POST,
                "/v1/audio/transcriptions",
                Map.<String, Collection<String>>of(),
                null,
                StandardCharsets.UTF_8,
                new RequestTemplate()
        );

        return new FeignException.BadGateway(
                "Whisper service unavailable",
                request,
                null,
                Map.of()
        );
    }
}
