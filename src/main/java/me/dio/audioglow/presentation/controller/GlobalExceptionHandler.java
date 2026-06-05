package me.dio.audioglow.presentation.controller;

import feign.FeignException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiErrorResponse> handleFeignException(FeignException exception) {
        var status = HttpStatus.BAD_GATEWAY;
        var error = new ApiErrorResponse(
                Instant.now(),
                "Nao foi possivel concluir a comunicacao com os servicos de IA.",
                externalServiceDetails(exception)
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgumentException(IllegalArgumentException exception) {
        var status = HttpStatus.BAD_REQUEST;
        var error = new ApiErrorResponse(
                Instant.now(),
                "A requisicao enviada e invalida.",
                exception.getMessage()
        );

        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception exception) {
        var status = HttpStatus.INTERNAL_SERVER_ERROR;
        var error = new ApiErrorResponse(
                Instant.now(),
                "Ocorreu um erro interno ao processar o comando de voz.",
                exception.getMessage()
        );

        return ResponseEntity.status(status).body(error);
    }

    private String externalServiceDetails(FeignException exception) {
        if (exception.status() > 0) {
            return "External service returned HTTP status %d.".formatted(exception.status());
        }

        return exception.getMessage();
    }

    public record ApiErrorResponse(
            Instant timestamp,
            String mensagem,
            String detalhes
    ) {
    }
}
