package ch.akop.homesystem.external.mastodon;

import ch.akop.homesystem.config.properties.MastodonProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;

@RequiredArgsConstructor
@Service
public class MastodonService {

    private final MastodonProperties properties;

    private WebClient apiWebClient;

    @PostConstruct
    public void initializeWebClients() {
        apiWebClient = WebClient.builder()
                .baseUrl("https://%s/api/".formatted(properties.getServer()))
                .defaultHeaders(header -> header.setBearerAuth(properties.getToken()))
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(1024 * 1024 * 10))
                .build();
    }


    @Async
    public void publishImage(String text, byte[] image) {
        var createdImage = postImage(image);
        postStatus(text, createdImage.getId());
    }

    private void postStatus(String text, String imageId) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("status", text);
        form.add("media_ids", "[%s]".formatted(imageId));

        apiWebClient.post()
                .uri("v1/statuses")
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private MediaCreateResponse postImage(byte[] image) {
        var form = new MultipartBodyBuilder();
        form.part("description", "An automated generated image");
        form.part("file", new ByteArrayResource(image))
                .contentType(MediaType.IMAGE_JPEG);

        return apiWebClient.post()
                .uri("v2/media")
                .body(BodyInserters.fromMultipartData(form.build()))
                .exchangeToMono(response -> {
                    return Mono.just(new MediaCreateResponse());
                })
                .block();
    }

}
