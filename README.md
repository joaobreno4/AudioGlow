# AudioGlow API 🎙️✨

**Descrição**: API REST em Spring Boot 3.x e Java 21 para um assistente de voz inteligente. A aplicação recebe um arquivo de áudio, transcreve o comando com OpenAI Whisper, envia o texto ao Google Gemini e retorna uma resposta consolidada para o cliente.

## Arquitetura

O projeto segue princípios de Clean Architecture para manter as regras de negócio desacopladas de frameworks, clientes HTTP e detalhes de entrega.

```text
src/main/java/me/dio/audioglow
├── domain
│   └── dto                  # Records de entrada e saída do domínio da aplicação
├── application
│   ├── usecase              # Contratos dos casos de uso
│   └── service              # Orquestração do fluxo Whisper -> Gemini -> API response
├── infrastructure
│   ├── client               # Integrações HTTP via Spring Cloud OpenFeign
│   └── config               # Configurações tipadas da aplicação
└── presentation
    └── controller           # Controllers REST e tratamento global de exceções
```

Essa divisão reduz acoplamento porque a camada de aplicação depende de contratos e DTOs próprios, enquanto detalhes externos como OpenFeign, APIs de IA e HTTP ficam nas bordas. Isso facilita testes unitários, troca de provedores externos e evolução incremental da API.

## Tecnologias Utilizadas

- Java 21
- Spring Boot 3
- Spring Cloud OpenFeign
- OpenAI Whisper
- Google Gemini
- Micrometer
- Prometheus
- Spring Boot Actuator
- Springdoc OpenAPI / Swagger UI
- JUnit 5
- Mockito

## Variáveis de Ambiente

Configure as credenciais antes de iniciar a aplicação:

```bash
export OPENAI_API_KEY="sua-chave-openai"
export GEMINI_API_KEY="sua-chave-gemini"
```

As chaves são lidas pelo `application.yml`:

```yaml
external-ai:
  openai:
    api-key: ${OPENAI_API_KEY:}
  gemini:
    api-key: ${GEMINI_API_KEY:}
```

## Executando o Projeto

Pré-requisitos:

- JDK 21
- Maven 3.8+

Com as variáveis de ambiente configuradas, execute:

```bash
mvn spring-boot:run
```

A API ficará disponível em:

```text
http://localhost:8080
```

## Documentação OpenAPI

Com a aplicação em execução, acesse:

```text
http://localhost:8080/swagger-ui.html
```

ou:

```text
http://localhost:8080/swagger-ui/index.html
```

O contrato OpenAPI em JSON fica disponível em:

```text
http://localhost:8080/v3/api-docs
```

## Endpoints Principais

### Assistente de Voz

```http
POST /api/v1/assistant/voice
Content-Type: multipart/form-data
```

Campo multipart:

| Campo | Tipo | Obrigatório | Descrição |
| --- | --- | --- | --- |
| `audioFile` | arquivo | sim | Arquivo de áudio contendo o comando de voz. |

Exemplo com `curl`:

```bash
curl -X POST "http://localhost:8080/api/v1/assistant/voice" \
  -H "Content-Type: multipart/form-data" \
  -F "audioFile=@command.wav"
```

Resposta de sucesso:

```json
{
  "textoTranscrito": "Acenda as luzes da sala",
  "respostaIA": "As luzes da sala foram acesas.",
  "timestamp": "2026-06-05T23:00:00Z"
}
```

### Observabilidade

```text
GET /actuator/health
GET /actuator/metrics
GET /actuator/prometheus
```

O endpoint `/actuator/prometheus` expõe as métricas no formato esperado pelo Prometheus.

## Métricas Customizadas

| Métrica | Tipo | Tags | Descrição |
| --- | --- | --- | --- |
| `audioglow.voice_assistant.processing.duration` | Timer | `application` | Tempo total de processamento de um comando de voz. |
| `audioglow.voice_assistant.external_api.duration` | Timer | `provider`, `application` | Latência das chamadas externas para Whisper e Gemini. |
| `audioglow.voice_assistant.requests.failed` | Counter | `application` | Quantidade de requisições que falharam durante o processamento do serviço. |

Exemplos de séries esperadas no Prometheus:

```text
audioglow_voice_assistant_processing_duration_seconds_count
audioglow_voice_assistant_processing_duration_seconds_sum
audioglow_voice_assistant_external_api_duration_seconds_count{provider="whisper"}
audioglow_voice_assistant_external_api_duration_seconds_count{provider="gemini"}
audioglow_voice_assistant_requests_failed_total
```

## Tratamento de Erros

Erros de integrações externas são tratados pelo `GlobalExceptionHandler` e retornam HTTP `502 Bad Gateway` com uma resposta padronizada:

```json
{
  "timestamp": "2026-06-05T23:00:00Z",
  "mensagem": "Nao foi possivel concluir a comunicacao com os servicos de IA.",
  "detalhes": "External service returned HTTP status 502."
}
```

Erros inesperados retornam HTTP `500 Internal Server Error`.

## Testes

Execute a suíte de testes com:

```bash
mvn test
```

Os testes unitários validam o fluxo principal do caso de uso, o envio do texto transcrito ao Gemini, a propagação de falhas do Whisper e o registro das métricas customizadas.

Criado por João Breno
