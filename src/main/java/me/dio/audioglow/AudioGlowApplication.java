package me.dio.audioglow;

import me.dio.audioglow.infrastructure.config.ExternalAiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
@EnableConfigurationProperties(ExternalAiProperties.class)
public class AudioGlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(AudioGlowApplication.class, args);
    }
}
