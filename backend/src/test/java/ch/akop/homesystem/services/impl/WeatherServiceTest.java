package ch.akop.homesystem.services.impl;

import io.reactivex.rxjava3.core.Observable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class WeatherServiceTest {

  @Test
  void retry() throws InterruptedException {
    var counter = new AtomicInteger(0);

    var counterError = new AtomicInteger(0);

    var observable = Observable.interval(100, TimeUnit.MILLISECONDS)
        .map(value -> {
          if (counter.get() < 15) {
            throw new RuntimeException("oh jeez");
          }

          return value;
        })
        .doOnSubscribe(ignored -> {
          counter.incrementAndGet();
          log.info("subscribed");
        })
        .retryWhen(errors$ ->
            errors$
                .scan(0, (numberOfTotalErrors, lastError) -> numberOfTotalErrors + 1)
                .map(numberOfTotalErrors -> {

                  if (numberOfTotalErrors == 10) {
                    log.info("Ich habs jetzt schon 10 mal probiert ...");
                  }

                  return Observable.timer((long) Math.min(Math.pow(4, numberOfTotalErrors), 180), TimeUnit.MILLISECONDS);
                }))
        .take(30)
        .subscribe(
            value -> log.info("got it " + value),
            throwable -> log.info("got a error"));

    Thread.sleep(20_000);
  }

}
