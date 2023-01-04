package ch.akop.homesystem.external.mastodon;

import ch.akop.homesystem.persistence.repository.config.MastodonConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
@Service
public class MastodonService {

    private final MastodonConfigRepository mastodonConfigRepository;

    private WebClient apiWebClient = WebClient.builder()
            .build();

    @Async
    public void publishImage(String text, byte[] image) {
        var createdImage = postImage(image);
        postStatus(text, createdImage.getId());
    }

    private void postStatus(String text, String imageId) {
        var form = new LinkedMultiValueMap<String, String>();
        form.add("status", text);
        form.add("media_ids[]", imageId);
        form.add("visibility", "public");
        form.add("language", "en");

        apiWebClient.post()
                .uri("v1/statuses")
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    private MediaCreateResponse postImage(byte[] image) {
        var form = new MultipartBodyBuilder();
        form.part("file", new ByteArrayResource(image))
                .contentType(MediaType.IMAGE_JPEG)
                .filename("image.jpg");

        return apiWebClient.post()
                .uri("v2/media")
                .body(BodyInserters.fromMultipartData(form.build()))
                .retrieve()
                .bodyToMono(MediaCreateResponse.class)
                .block();
    }

}
