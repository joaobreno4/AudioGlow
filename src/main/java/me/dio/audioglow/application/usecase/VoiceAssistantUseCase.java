package me.dio.audioglow.application.usecase;

import me.dio.audioglow.domain.dto.VoiceAssistantResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VoiceAssistantUseCase {

    VoiceAssistantResponse processVoiceCommand(MultipartFile audioFile);
}
