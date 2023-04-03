package ch.akop.homesystem.external.openai;

import ch.akop.homesystem.persistence.model.config.OpenAIConfig;
import ch.akop.homesystem.persistence.repository.config.OpenAIConfigRepository;
import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.rest.client.ext.ClientHeadersFactory;

@ApplicationScoped
@RequiredArgsConstructor
public class OpenAIHeader implements ClientHeadersFactory {

  private final OpenAIConfigRepository openAIConfigRepository;

  @Override
  public MultivaluedMap<String, String> update(MultivaluedMap<String, String> incomingHeaders, MultivaluedMap<String, String> clientOutgoingHeaders) {
    var result = new MultivaluedHashMap<String, String>();
    var token = openAIConfigRepository.findFirstByOrderByModifiedDesc()
        .map(OpenAIConfig::getApiKey)
        .orElseThrow(() -> new IllegalStateException("No API key"));
    result.add("Authorization", "Bearer " + token);
    return result;
  }
}
