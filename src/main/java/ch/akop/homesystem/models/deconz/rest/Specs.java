package ch.akop.homesystem.models.deconz.rest;

import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class Specs {

    public static Mono<SensorResponse> getAllSensors(WebClient client) {
        return client.get().uri("/sensors").retrieve().bodyToMono(SensorResponse.class);
    }

    public static Mono<LightResponse> getAllLights(WebClient client) {
        return client.get().uri("/lights").retrieve().bodyToMono(LightResponse.class);
    }

    public static Mono<ResponseEntity<Void>> setLight(String id, UpdateLightParameters updateLightParameters, WebClient client) {
        return client.put().uri("/lights/%s/state".formatted(id))
                .body(Mono.just(updateLightParameters), UpdateLightParameters.class)
                .retrieve()
                .toBodilessEntity();
    }

}
