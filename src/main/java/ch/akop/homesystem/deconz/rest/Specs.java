package ch.akop.homesystem.deconz.rest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Specs {

    public static Mono<SensorResponse> getAllSensors(final WebClient client) {
        return client.get().uri("/sensors").retrieve().bodyToMono(SensorResponse.class);
    }

    public static Mono<LightResponse> getAllLights(final WebClient client) {
        return client.get().uri("/lights").retrieve().bodyToMono(LightResponse.class);
    }

    public static Mono<ResponseEntity<Void>> setLight(final String id, final UpdateLightParameters updateLightParameters, final WebClient client) {
        return client.put().uri("/lights/%s/state".formatted(id))
                .body(Mono.just(updateLightParameters), UpdateLightParameters.class)
                .retrieve()
                .toBodilessEntity();
    }

}
