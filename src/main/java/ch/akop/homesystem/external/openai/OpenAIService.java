package ch.akop.homesystem.external.openai;

import ch.akop.homesystem.config.properties.OpenAIProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAIProperties openAIProperties;
    private WebClient apiWebClient;


    @PostConstruct
    protected void initializeWebClients() {
        apiWebClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/")
                .defaultHeaders(header -> header.setBearerAuth(openAIProperties.getApiKey()))
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(1024 * 1024 * 10))
                .build();
    }

    @SneakyThrows
    public Mono<byte[]> requestImage(String text) {
        var requestBody = new ImageRequest()
                .setResponseFormat(ImageRequest.ResponseFormat.B64_JSON)
                .setN(1)
                .setSize(openAIProperties.getSize())
                .setPrompt(text);

        log.info("Request a {} open-ai image for: {}", requestBody.getSize(), text);

        return apiWebClient.post()
                .uri("images/generations")
                .body(BodyInserters.fromValue(requestBody))
                .headers(header -> header.setContentType(MediaType.APPLICATION_JSON))
                .retrieve()
                .bodyToMono(Response.class)
                .map(response -> response.getData().get(0).getB64_json())
                .map(b64 -> Base64.getDecoder().decode(b64));
    }

}
