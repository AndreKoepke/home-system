package ch.akop.homesystem.external.mastodon;

import ch.akop.homesystem.persistence.repository.config.MastodonConfigRepository;
import java.net.URI;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.RestClientBuilder;


@RequiredArgsConstructor
@ApplicationScoped
@Slf4j
public class MastodonService {

  private final MastodonConfigRepository mastodonConfigRepository;

  private MastodonServiceSpec apiWebClient;

  @PostConstruct
  public void initializeWebClients() {
    mastodonConfigRepository.findFirstByOrderByModifiedDesc()
        .ifPresent(properties -> apiWebClient = RestClientBuilder.newBuilder()
            .baseUri(URI.create("https://%s/".formatted(properties.getServer())))
            .build(MastodonServiceSpec.class)
        );
  }


  public void publishImage(String text, byte[] image) {
    if (apiWebClient == null) {
      log.warn("Tried to post mastodon-image, but there no mastodon-config. This call will be ignored");
      return;
    }

    var createdImage = apiWebClient.postImage(image, "test.jpg");
    apiWebClient.postStatus(text, createdImage.getId(), "en", "public");
  }


}
