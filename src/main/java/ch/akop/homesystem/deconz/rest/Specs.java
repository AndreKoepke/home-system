package ch.akop.homesystem.deconz.rest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Specs {

    public static Mono<Sensors> getAllSensors(WebClient client) {
        return client.get().uri("/sensors")
                .retrieve()
                .bodyToMono(Sensors.class)
                .retryWhen(retry5xxErrors());
    }

    public static Mono<Lights> getAllLights(WebClient client) {
        return client.get().uri("/lights")
                .retrieve()
                .bodyToMono(Lights.class)
                .retryWhen(retry5xxErrors());
    }

    public static Mono<Groups> getAllGroups(WebClient client) {
        return client.get().uri("/groups")
                .retrieve()
                .bodyToMono(Groups.class)
                .retryWhen(retry5xxErrors());
    }

    public static Mono<ResponseEntity<Void>> setState(String id,
                                                      State updateLightParameters,
                                                      WebClient client) {
        return client.put().uri("/lights/%s/state".formatted(id))
                .body(Mono.just(updateLightParameters), State.class)
                .retrieve()
                .toBodilessEntity()
                .retryWhen(retry5xxErrors());
    }

    public static Mono<ResponseEntity<Void>> activateScene(String groupId,
                                                           String sceneId,
                                                           WebClient client) {
        return client.put().uri("/groups/%s/scenes/%s/recall".formatted(groupId, sceneId))
                .retrieve()
                .toBodilessEntity()
                .retryWhen(retry5xxErrors());
    }

    private static RetryBackoffSpec retry5xxErrors() {
        return Retry.backoff(3, Duration.ofSeconds(3))
                .filter(throwable -> {
                    if (throwable instanceof WebClientResponseException e) {
                        return e.getStatusCode().is5xxServerError();
                    }

                    return false;
                });
    }


}
