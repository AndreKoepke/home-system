package ch.akop.homesystem.deconz.rest;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Specs {

    private static final RateLimiter rateLimit = RateLimiter.of("writing-limit", RateLimiterConfig.custom()
            .limitRefreshPeriod(Duration.of(100, ChronoUnit.MILLIS))
            .limitForPeriod(1)
            .build());

    public static Mono<Sensors> getAllSensors(final WebClient client) {
        return client.get().uri("/sensors")
                .retrieve()
                .bodyToMono(Sensors.class)
                .retryWhen(retry5xxErrors());
    }

    public static Mono<Lights> getAllLights(final WebClient client) {
        return client.get().uri("/lights")
                .retrieve()
                .bodyToMono(Lights.class)
                .retryWhen(retry5xxErrors());
    }

    public static Mono<Groups> getAllGroups(final WebClient client) {
        return client.get().uri("/groups")
                .retrieve()
                .bodyToMono(Groups.class)
                .retryWhen(retry5xxErrors());
    }

    public static Mono<ResponseEntity<Void>> setLight(final String id,
                                                      final UpdateLightParameters updateLightParameters,
                                                      final WebClient client) {
        return client.put().uri("/lights/%s/state".formatted(id))
                .body(Mono.just(updateLightParameters), UpdateLightParameters.class)
                .retrieve()
                .toBodilessEntity()
                .transformDeferred(RateLimiterOperator.of(rateLimit))
                .retryWhen(retry5xxErrors());
    }

    public static Mono<ResponseEntity<Void>> activateScene(final String groupId,
                                                           final String sceneId,
                                                           final WebClient client) {
        return client.put().uri("/groups/%s/scenes/%s/recall".formatted(groupId, sceneId))
                .retrieve()
                .toBodilessEntity()
                .transformDeferred(RateLimiterOperator.of(rateLimit))
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
