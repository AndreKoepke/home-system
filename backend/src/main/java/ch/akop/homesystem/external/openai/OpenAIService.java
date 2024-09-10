package ch.akop.homesystem.external.openai;

import ch.akop.homesystem.external.openai.TextGenerationParameter.Message;
import ch.akop.homesystem.persistence.repository.config.OpenAIConfigRepository;
import java.net.URI;
import java.util.Base64;
import java.util.List;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
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

    return Base64.getDecoder().decode(apiWebClient.requestImage(requestBody).getData().get(0).getB64_json());
  }

  @Transactional
  public String requestText(String prompt) {
    var config = openAIConfigRepository.findFirstByOrderByModifiedDesc()
        .orElse(null);

    if (config == null) {
      log.warn("Text requested, but openAI is not configured. Ignoring.");
      return null;
    }

    var requestBody = new TextGenerationParameter()
        .setMessages(List.of(
            new Message("system", "Du bist ein irrer Wissenschaftler und liebst es kurze und verrückte Antworten zu geben."),
            new Message("user", prompt)));

    return apiWebClient.textCompletion(requestBody).getChoices().get(0).getMessage().getContent();
  }

}
