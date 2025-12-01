package ch.akop.homesystem.external.openai;

import static java.util.Optional.empty;

import ch.akop.homesystem.external.openai.TextGenerationParameter.Message;
import ch.akop.homesystem.persistence.repository.config.OpenAIConfigRepository;
import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

  private final OpenAIConfigRepository openAIConfigRepository;

  private final OpenAIServiceSpec apiWebClient = RestClientBuilder.newBuilder()
      .baseUri(URI.create("https://api.openai.com"))
      .build(OpenAIServiceSpec.class);

  @Transactional
  @Nullable
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

    return Base64.getDecoder().decode(apiWebClient.requestImage(requestBody).getData().getFirst().getB64_json());
  }

  @Transactional
  public Optional<String> requestText(String prompt) {
    return openAIConfigRepository.findFirstByOrderByModifiedDesc()
        .map(config -> {
          var requestBody = new TextGenerationParameter()
              .setMessages(List.of(
                  new Message("system", "Du bist ein irrer Wissenschaftler und liebst es verrückte Antworten zu geben. Antworte in wenigen Sätzen, am besten nur mit einem Satz."),
                  new Message("system", "Du sollst folgende Nachricht dem User überbringen: " + prompt)));

          return apiWebClient.textCompletion(requestBody).getChoices().getFirst().getMessage().getContent();
        }).or(() -> {
          log.warn("Text requested, but openAI is not configured. Ignoring.");
          return empty();
        });
  }
}
