package ch.akop.homesystem.external.openai;

import ch.akop.homesystem.persistence.repository.config.OpenAIConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.net.URI;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    private final OpenAIConfigRepository openAIConfigRepository;

    private final OpenAIServiceSpec apiWebClient = RestClientBuilder.newBuilder()
            .baseUri(URI.create("https://api.openai.com"))
            .build(OpenAIServiceSpec.class);

    @Transactional
    public byte[] requestImage(String text) {
        var config = openAIConfigRepository.findFirstByOrderByModifiedDesc()
                .orElse(null);

        if (config == null) {
            log.warn("Image requested, but openAI is not configured. Ignoring.");
            return null;
        }

        var requestBody = new ImageRequest()
                .setResponseFormat(ImageRequest.ResponseFormat.B64_JSON)
                .setN(1)
                .setSize(config.getSize())
                .setPrompt(text);

        log.info("Request a {} open-ai image for: {}", requestBody.getSize(), text);

        return Base64.getDecoder().decode(apiWebClient.requestImage(requestBody).getData().get(0).getB64_json());
    }

}
