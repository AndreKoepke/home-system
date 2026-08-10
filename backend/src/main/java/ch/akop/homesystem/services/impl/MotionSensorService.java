package ch.akop.homesystem.services.impl;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static java.time.temporal.ChronoUnit.SECONDS;

import ch.akop.homesystem.controller.dtos.MotionSensorDto.ConfigDto;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.RollerShutter;
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
import jakarta.annotation.Priority;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;

@RequiredArgsConstructor
@Priority(500)
@Singleton
@Slf4j
public class MotionSensorService {

  private final MotionSensorConfigRepository motionSensorConfigRepository;
  private final AnimationService animationService;
  private final DeviceService deviceService;
  private final StateService stateService;
  private final WeatherService weatherService;
  private final EventBus eventBus;
  private final Set<String> sensorsWithHigherTimeout = new HashSet<>();

  private Map<String, ConfigWithLights> sensors;

  @Transactional
  public void init() {
    sensors = motionSensorConfigRepository.findAll().stream()
        .filter(config -> {
          var foundMovementSensor = deviceService.findDeviceByName(config.getName(), MotionSensor.class).isPresent();
          if (!foundMovementSensor) {
            log.warn("MotionSensor with name {} not found", config.getName());
            return false;
          }
          return true;
        })
        .map(ConfigWithLights::new)
        .collect(Collectors.toMap(
            configWithLights -> configWithLights.config.getName(),
            Function.identity()));

    sensors.values().forEach(ConfigWithLights::startListing);
  }

  public void requestHigherTimeout(String sensorName) {
    sensorsWithHigherTimeout.add(sensorName.toLowerCase());
  }

  public boolean isHigherTimeoutRequested(MotionSensorConfig motionSensorConfig) {
    return sensorsWithHigherTimeout.contains(motionSensorConfig.getName().toLowerCase());
  }

  public void update(ConfigDto configDto) {
    var config = dtoToInternal(configDto);
    motionSensorConfigRepository.save(config);
    sensors.get(config.getName()).config = config;
  }

  private MotionSensorConfig dtoToInternal(ConfigDto config) {
    return MotionSensorConfig.builder()
        .name(config.getName())
        .lights(config.getLights())
        .lightsAtNight(config.getLightsAtNight())
        .keepMovingFor(config.getKeepMovingFor())
        .onlyTurnOnWhenDarkerAs(config.getOnlyTurnOnWhenDarkerAs())
        .selfLightNoise(config.getSelfLightNoise())
        .turnLightOnWhenMovement(config.isTurnLightOnWhenMovement())
        .turnOnWhenRollerShutterIsClosed(config.getTurnOnWhenRollerShutterIsClosed())
        .notBefore(config.getNotBefore())
        .animation(Optional.ofNullable(config.getAnimationId())
            .flatMap(animationService::findById)
            .orElse(null))
        .animationNight(Optional.ofNullable(config.getAnimationAtNightId())
            .flatMap(animationService::findById)
            .orElse(null))
        .build();
  }

  public class ConfigWithLights {

    private final MotionSensor sensor;
    private MotionSensorConfig config;
    private boolean movementDetected = false;

    public ConfigWithLights(MotionSensorConfig config) {
      this.config = config;
      eagerFetchAllLazyCollections(config);
      this.sensor = MotionSensorService.this.deviceService.findDeviceByName(config.getName(), MotionSensor.class)
          .orElseThrow(() -> new NoSuchElementException("MotionSensor '" + config.getName() + "' not found"));
    }

    private void handleStateChanged(boolean isSleepState) {
      if (movementDetected) {
        turnOff(!isSleepState); // turn lights of previous state off
        turnOn(isSleepState); // turn lights of new state on
      }
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

    public void startListing() {
      sensor.getIsMoving$()
          .subscribeOn(Schedulers.io())
          .withLatestFrom(getIsBright$(), MovementAndLux::new)
          .distinctUntilChanged()
          .filter(this::shouldIgnoreMotionEvent)
          .filter(this::blockMovingWhenNecessary)
          .switchMap(this::delayWhenNoMovement)
          .subscribe(this::handleMotionEvent);

      stateService.getCurrentState$()
          .skip(1)
          .map(SleepState.class::isInstance)
          .subscribe(this::handleStateChanged);
    }

    public Observable<Boolean> getIsBright$() {
      if (sensor.getLightLevel() != null) {
        return sensor.getLightLevel().getLux$()
            .withLatestFrom(shouldCloseBecauseOfRollerShutter(), WeatherAndRollerShutter::new)
            .map(this::isMatchingWeather)
            .throttleFirst(1, TimeUnit.MINUTES);
      }

      return weatherService.getWeather()
          .map(weather -> weather.getLight().getAs(KILO_LUX).intValue())
          .withLatestFrom(shouldCloseBecauseOfRollerShutter(), WeatherAndRollerShutter::new)
          .map(this::isMatchingWeather);
    }

    private record WeatherAndRollerShutter(int lux, boolean shouldCloseBecauseOfRollerShutter) {

    }

    private Observable<Boolean> shouldCloseBecauseOfRollerShutter() {
      if (config.getTurnOnWhenRollerShutterIsClosed() == null) {
        return Observable.just(false);
      }

      return deviceService.findDeviceByName(config.getTurnOnWhenRollerShutterIsClosed(), RollerShutter.class)
          .map(rollerShutter -> rollerShutter.getLift$()
              .map(lift -> lift < 10)
              .distinctUntilChanged())
          .orElse(Observable.just(false));
    }

    private Stream<SimpleLight> getAffectedLights() {
      var lightNames = stateService.isState(SleepState.class)
          ? config.getLightsAtNight()
          : config.getLights();

      return lightNames.stream()
          .flatMap(lightName -> deviceService.findDeviceByName(lightName, SimpleLight.class).stream());
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

    private boolean isMatchingWeather(WeatherAndRollerShutter luxAndRollerShutter) {
      if (config.getOnlyTurnOnWhenDarkerAs() == null) {
        return true;
      }

      if (luxAndRollerShutter.shouldCloseBecauseOfRollerShutter) {
        return true;
      }

      var anyLightOn = getAffectedLights().anyMatch(SimpleLight::isCurrentStateIsOn);
      int lux;
      if (config.getSelfLightNoise() != null && anyLightOn) {
        lux = luxAndRollerShutter.lux - config.getSelfLightNoise();
      } else {
        lux = luxAndRollerShutter.lux;
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
      if (update.isMoving() || config.getKeepMovingFor() == null) {
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
        turnOff(stateService.isState(SleepState.class));
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
        turnOn(stateService.isState(SleepState.class));
      } else {
        turnOff(stateService.isState(SleepState.class));
      }
    }

    private void turnOn(boolean isSleepState) {
      if (isSleepState && config.getAnimationNight() != null) {
        eventBus.publish("home/animation/play", config.getAnimationNight().getId());
      } else if (!isSleepState && config.getAnimation() != null) {
        eventBus.publish("home/animation/play", config.getAnimation().getId());
      } else {
        turnOnWithoutAnimation();
      }
    }

    private void turnOff(boolean isSleepState) {
      if (isSleepState && config.getAnimationNight() != null) {
        eventBus.publish("home/animation/turn-off", config.getAnimationNight().getId());
      } else if (!isSleepState && config.getAnimation() != null) {
        eventBus.publish("home/animation/turn-off", config.getAnimation().getId());
      } else {
        turnOffWithoutAnimation();
        sensorsWithHigherTimeout.remove(config.getName().toLowerCase());
      }
    }

    private void turnOnWithoutAnimation() {
      getAffectedLights()
          .filter(simpleLight -> !simpleLight.isCurrentStateIsOn())
          .forEach(light -> {
            if (light instanceof DimmableLight dimmable) {
              if (stateService.isState(SleepState.class)) {
                dimmable.setBrightness(10, Duration.of(10, SECONDS));
              } else {
                dimmable.setBrightness(100, Duration.of(10, SECONDS));
              }
            } else {
              light.turnOn();
            }
          });
    }

    private void turnOffWithoutAnimation() {
      getAffectedLights()
          .filter(SimpleLight::isCurrentStateIsOn)
          .forEach(SimpleLight::turnOff);
    }

    public record MovementAndLux(
        boolean isMoving,
        boolean shouldBeOnBecauseOfBrightness
    ) {

    }
  }

}


