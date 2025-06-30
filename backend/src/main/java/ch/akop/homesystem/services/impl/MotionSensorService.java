package ch.akop.homesystem.services.impl;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;

import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.sensor.MotionSensor;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.persistence.model.config.MotionSensorConfig;
import ch.akop.homesystem.persistence.repository.config.MotionSensorConfigRepository;
import ch.akop.homesystem.states.NormalState;
import ch.akop.homesystem.states.SleepState;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.vertx.core.eventbus.EventBus;
import java.time.Duration;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;

@RequiredArgsConstructor
@Priority(500)
@Dependent
@Slf4j
public class MotionSensorService {

  private final MotionSensorConfigRepository motionSensorConfigRepository;
  private final DeviceService deviceService;
  private final StateService stateService;
  private final WeatherService weatherService;
  private final EventBus eventBus;
  private final Set<String> sensorsWithHigherTimeout = new HashSet<>();

  @Transactional
  public void init() {
    // TODO restart when config changes
    motionSensorConfigRepository.findAll().stream()
        .filter(config -> {
          var foundMovementSensor = deviceService.findDeviceByName(config.getName(), MotionSensor.class).isPresent();
          if (!foundMovementSensor) {
            log.warn("MotionSensor with name {} not found", config.getName());
            return false;
          }
          return true;
        })
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
    private List<SimpleLight> referencedLights;
    private boolean movementDetected = false;

    public ConfigWithLights(MotionSensorConfig config) {
      this.config = config;
      eagerFetchAllLazyCollections(config);
      this.referencedLights = resolveLights();
      this.sensor = MotionSensorService.this.deviceService.findDeviceByName(config.getName(), MotionSensor.class)
          .orElseThrow(() -> new NoSuchElementException("MotionSensor '" + config.getName() + "' not found"));
    }

    private void eagerFetchAllLazyCollections(MotionSensorConfig motionSensorConfig) {
      Hibernate.initialize(motionSensorConfig.getLights());
      Hibernate.initialize(motionSensorConfig.getLightsAtNight());
      Optional.ofNullable(motionSensorConfig.getAnimation()).ifPresent(this::eagerFetchAllLazyCollections);
      Optional.ofNullable(motionSensorConfig.getAnimationNight()).ifPresent(this::eagerFetchAllLazyCollections);
    }

    private void eagerFetchAllLazyCollections(Animation animation) {
      Hibernate.initialize(animation.getLights());
      Hibernate.initialize(animation.getDimmLightSteps());
      Hibernate.initialize(animation.getPauseSteps());
      Hibernate.initialize(animation.getOnOffSteps());
    }

    private List<SimpleLight> resolveLights() {
      return config.getAffectedLightNames(stateService.isState(SleepState.class))
          .stream()
          .flatMap(lightName -> MotionSensorService.this.deviceService.findDeviceByName(lightName, SimpleLight.class).stream())
          .toList();
    }

    public void startListing() {
      stateService.getCurrrentState$()
          .skip(1)
          .subscribe(newState -> this.referencedLights = resolveLights());

      sensor.getIsMoving$()
          .subscribeOn(Schedulers.io())
          .withLatestFrom(getIsBright$(), MovementAndLux::new)
          .distinctUntilChanged()
          .filter(this::shouldIgnoreMotionEvent)
          .filter(this::blockMovingWhenNecessary)
          .switchMap(this::delayWhenNoMovement)
          .subscribe(this::handleMotionEvent);
    }

    public Observable<Boolean> getIsBright$() {
      if (sensor.getLightLevel() != null) {
        return sensor.getLightLevel().getLux$()
            .map(this::isMatchingWeather)
            .throttleFirst(1, TimeUnit.MINUTES);
      }

      return weatherService.getWeather()
          .map(weather -> weather.getLight().getAs(KILO_LUX).intValue())
          .map(this::isMatchingWeather);
    }

    public void turnAllLightsOff() {
      referencedLights
          .stream().filter(SimpleLight::isCurrentStateIsOn)
          .forEach(SimpleLight::turnOff);
    }

    private void turnAllLightsOn() {
      referencedLights.stream()
          .filter(simpleLight -> !simpleLight.isCurrentStateIsOn())
          .forEach(light -> {
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

    private boolean shouldIgnoreMotionEvent(MovementAndLux update) {
      if (!update.isMoving()) {
        return true;
      }

      return config.isTurnLightOnWhenMovement();
    }

    private boolean blockMovingWhenNecessary(MovementAndLux update) {

      if (!update.isMoving()) {
        // don't block when movement stops
        return true;
      }

      return isMatchingTime()
          && isMatchingState();
    }

    private boolean isMatchingWeather(int lux) {
      if (config.getOnlyTurnOnWhenDarkerAs() == null) {
        return true;
      }

      if (config.getSelfLightNoise() != null && referencedLights.stream().anyMatch(SimpleLight::isCurrentStateIsOn)) {
        lux -= config.getSelfLightNoise();
      }

      return lux < config.getOnlyTurnOnWhenDarkerAs();
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

    public Observable<MovementAndLux> delayWhenNoMovement(MovementAndLux update) {
      if (Boolean.TRUE.equals(update.isMoving()) || config.getKeepMovingFor() == null) {
        // no delay
        return Observable.just(update);
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
              return Observable.just(update).delay(timeout * 2, TimeUnit.SECONDS);
            }

            return Observable.just(update);
          });
    }

    private void handleMotionEvent(MovementAndLux update) {
      if (!update.shouldBeOnBecauseOfBrightness && movementDetected) {
        turnOff();
        movementDetected = false;
        return;
      } else if (!update.shouldBeOnBecauseOfBrightness) {
        return;
      }

      if (movementDetected && update.isMoving) {
        return;
      }
      movementDetected = update.isMoving;

      if (movementDetected) {
        turnOn();
      } else {
        turnOff();
      }
    }


    private void turnOn() {
      if (stateService.isState(SleepState.class) && config.getAnimationNight() != null) {
        eventBus.publish("home/animation/play", config.getAnimationNight().getId());
      } else if (!stateService.isState(SleepState.class) && config.getAnimation() != null) {
        eventBus.publish("home/animation/play", config.getAnimation().getId());
      } else {
        turnAllLightsOn();
      }
    }

    private void turnOff() {
      if (stateService.isState(SleepState.class) && config.getAnimationNight() != null) {
        eventBus.publish("home/animation/turn-off", config.getAnimationNight().getId());
      } else if (!stateService.isState(SleepState.class) && config.getAnimation() != null) {
        eventBus.publish("home/animation/turn-off", config.getAnimation().getId());
      } else {
        turnAllLightsOff();
        sensorsWithHigherTimeout.remove(config.getName().toLowerCase());
      }
    }


    public record MovementAndLux(
        boolean isMoving,
        boolean shouldBeOnBecauseOfBrightness
    ) {

    }
  }

}


