package me.dio.audioglow.infrastructure.client;

import me.dio.audioglow.domain.dto.WhisperTranscriptionResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@FeignClient(
        name = "whisperClient",
        url = "${external-ai.openai.base-url:https://api.openai.com}"
)
public interface WhisperClient {

    @PostMapping(
            value = "/v1/audio/transcriptions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    WhisperTranscriptionResponse transcribe(
            @RequestHeader("Authorization") String authorization,
            @RequestPart("file") MultipartFile file,
            @RequestPart("model") String model
    );
}
