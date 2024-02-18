package ch.akop.homesystem.services.impl;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.persistence.model.config.MotionSensorConfig;
import ch.akop.homesystem.persistence.repository.config.MotionSensorConfigRepository;
import ch.akop.homesystem.states.NormalState;
import ch.akop.homesystem.states.SleepState;
import io.reactivex.rxjava3.core.Observable;
import io.vertx.core.eventbus.EventBus;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@ApplicationScoped
@RequiredArgsConstructor
public class MotionSensorService {

  private final MotionSensorConfigRepository motionSensorConfigRepository;
  private final DeviceService deviceService;
  private final StateService stateService;
  private final WeatherService weatherService;
  private final EventBus eventBus;
  private final Set<String> sensorsWithHigherTimeout = new HashSet<>();

  @PostConstruct
  @Transactional
  public void init() {
    // TODO restart when config changes
    motionSensorConfigRepository.findAll().stream()
        .map(ConfigWithLights::new)
        .forEach(ConfigWithLights::startListing);
  }

  public void requestHigherTimeout(String sensorName) {
    sensorsWithHigherTimeout.add(sensorName.toLowerCase());
  }

  public boolean isHigherTimeoutRequested(MotionSensorConfig motionSensorConfig) {
    return sensorsWithHigherTimeout.contains(motionSensorConfig.getName().toLowerCase());
  }


  public class ConfigWithLights {

    private final MotionSensor sensor;
    private final MotionSensorConfig config;
    private final List<SimpleLight> referencedLights;
    private boolean movementDetected = false;

    public ConfigWithLights(MotionSensorConfig config) {
      this.config = config;
      this.referencedLights = config.getAffectedLightNames().stream()
          .flatMap(lightName -> MotionSensorService.this.deviceService.findDeviceByName(lightName, SimpleLight.class).stream())
          .toList();
      this.sensor = MotionSensorService.this.deviceService.findDeviceByName(config.getName(), MotionSensor.class)
          .orElseThrow(() -> new NoSuchElementException("MotionSensor '" + config.getName() + "' not found"));
    }

    public void startListing() {
      sensor.getIsMoving$()
          .filter(this::shouldIgnoreMotionEvent)
          .filter(this::blockMovingWhenNecessary)
          .switchMap(this::delayWhenNoMovement)
          .subscribe(this::handleMotionEvent);
    }

    public void turnAllLightsOff() {
      referencedLights.forEach(SimpleLight::turnOff);
    }

    private void turnAllLightsOn() {
      referencedLights.forEach(light -> {
        if (light instanceof DimmableLight dimmable) {
          if (stateService.getCurrentState() instanceof SleepState) {
            dimmable.setBrightness(10, Duration.of(10, ChronoUnit.SECONDS));
          } else {
            dimmable.setBrightness(100, Duration.of(10, ChronoUnit.SECONDS));
          }
        } else {
          light.turnOn();
        }
      });
    }

    private boolean shouldIgnoreMotionEvent(Boolean isMoving) {
      if (!isMoving) {
        return true;
      }

      return config.isTurnLightOnWhenMovement();
    }

    private boolean blockMovingWhenNecessary(boolean isMoving) {

      if (!isMoving) {
        // don't block when movement stops
        return true;
      }

      return isMatchingTime()
          && isMatchingState()
          && isMatchingWeather();
    }

    private boolean isMatchingWeather() {
      if (config.getOnlyTurnOnWhenDarkerAs() == null) {
        return true;
      }

      return weatherService.getWeather()
          .take(1)
          .blockingFirst()
          .getLight()
          .isSmallerThan(config.getOnlyTurnOnWhenDarkerAs(), KILO_LUX);
    }

    private boolean isMatchingState() {
      if (config.getOnlyAtNormalState() == null || !config.getOnlyAtNormalState()) {
        return true;
      }

      return stateService.isState(NormalState.class);
    }

    private boolean isMatchingTime() {
      if (config.getNotBefore() == null) {
        return true;
      }

      return config.getNotBefore().isBefore(LocalTime.now());
    }

    public Observable<Boolean> delayWhenNoMovement(Boolean movementDetected) {
      if (Boolean.TRUE.equals(movementDetected)) {
        // don't delay, when movement was detected
        return Observable.just(true);
      }

      // but if not movement detected, then wait
      var timeout = isHigherTimeoutRequested(config)
          ? config.getKeepMovingFor().toSeconds() * 3
          : config.getKeepMovingFor().toSeconds();

      return Observable.just(false)
          .delay(timeout, TimeUnit.SECONDS)
          .switchMap(ignored -> {
            if (isHigherTimeoutRequested(config)) {
              // if a timeout requested while waiting for the old timeout,
              // then increase the timeout
              return Observable.just(false).delay(timeout * 2, TimeUnit.SECONDS);
            }

            return Observable.just(false);
          });
    }

    private void handleMotionEvent(boolean isMoving) {
      if (movementDetected && isMoving) {
        return;
      }
      movementDetected = isMoving;
      if (config.getAnimation() == null) {
        handleMotionEventLightsTarget(isMoving);
      } else {
        handleMotionEventAnimationTarget(isMoving);
      }
    }

    private void handleMotionEventAnimationTarget(boolean isMoving) {
      if (isMoving) {
        eventBus.publish("home/animation/play", config.getAnimation());
      } else {
        eventBus.publish("home/animation/turn-off", config.getAnimation());
      }
    }

    private void handleMotionEventLightsTarget(boolean isMoving) {
      if (isMoving) {
        turnAllLightsOn();
      } else {
        turnAllLightsOff();
        sensorsWithHigherTimeout.remove(config.getName().toLowerCase());
      }
    }
  }

}


