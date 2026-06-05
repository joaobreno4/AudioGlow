package me.dio.audioglow.presentation.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import me.dio.audioglow.application.usecase.VoiceAssistantUseCase;
import me.dio.audioglow.domain.dto.VoiceAssistantResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/assistant")
public class VoiceAssistantController {

    private final VoiceAssistantUseCase voiceAssistantUseCase;

    public VoiceAssistantController(VoiceAssistantUseCase voiceAssistantUseCase) {
        this.voiceAssistantUseCase = voiceAssistantUseCase;
    }

    @PostMapping(
            value = "/voice",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Processa um comando de voz",
            description = "Recebe um arquivo de audio via multipart/form-data, transcreve com OpenAI Whisper, envia o texto ao Google Gemini e retorna a transcricao com a resposta da IA.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Comando de voz processado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = VoiceAssistantResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Arquivo de audio ausente ou invalido.",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "502",
                            description = "Falha de comunicacao com Whisper ou Gemini.",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Erro interno inesperado.",
                            content = @Content
                    )
            }
    )
    public ResponseEntity<VoiceAssistantResponse> processVoiceCommand(
            @Parameter(
                    description = "Arquivo de audio contendo o comando de voz a ser processado.",
                    required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
            )
            @RequestPart("audioFile") MultipartFile audioFile
    ) {
        var response = voiceAssistantUseCase.processVoiceCommand(audioFile);
        return ResponseEntity.ok(response);
    }
}
