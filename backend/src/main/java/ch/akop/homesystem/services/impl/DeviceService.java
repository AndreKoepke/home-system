package ch.akop.homesystem.services.impl;

import static java.time.temporal.ChronoUnit.MILLIS;
import static java.time.temporal.ChronoUnit.SECONDS;

import ch.akop.homesystem.models.devices.Device;
import ch.akop.homesystem.models.devices.actor.DimmableLight;
import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.models.devices.other.Group;
import ch.akop.homesystem.models.devices.other.Scene;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.persistence.model.config.BasicConfig;
import ch.akop.homesystem.persistence.repository.config.AnimationRepository;
import ch.akop.homesystem.persistence.repository.config.BasicConfigRepository;
import ch.akop.homesystem.util.SleepUtil;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@RequiredArgsConstructor
@ApplicationScoped
@Slf4j
public class DeviceService {

  private final List<Device<?>> devices = new ArrayList<>();
  private final Map<UUID, Disposable> runningAnimations = new ConcurrentHashMap<>();
  private final BasicConfigRepository basicConfigRepository;
  private final AnimationRepository animationRepository;
  private final Vertx vertx;

  private Set<String> notLights;


  @PostConstruct
  @Transactional
  void setNotLights() {
    notLights = basicConfigRepository.findByOrderByModifiedDesc()
        .map(BasicConfig::getNotLights)
        .orElse(new HashSet<>());
  }


  public <T extends Device<?>> Optional<T> findDeviceByName(String name, Class<T> clazz) {
    return getDevicesOfType(clazz)
        .stream()
        .filter(device -> device.getName().equalsIgnoreCase(name))
        .findFirst();
  }

  public <T extends Device<?>> Optional<T> findDeviceById(String name, Class<T> clazz) {
    return getDevicesOfType(clazz)
        .stream()
        .filter(device -> device.getId().equals(name))
        .findFirst();
  }

  public <T extends Device<?>> void registerDevice(T device) {
    devices.add(device);
  }


  public Collection<Device<?>> getAllDevices() {
    return new ArrayList<>(devices);
  }


  public <T extends Device<?>> Collection<T> getDevicesOfType(Class<T> clazz) {
    return devices.stream()
        .filter(clazz::isInstance)
        .map(clazz::cast)
        .collect(Collectors.toSet());
  }

  @Transactional
  public void turnAllLightsOff() {
    var notLights = basicConfigRepository.findByOrderByModifiedDesc()
        .map(BasicConfig::getNotLights)
        .orElse(new HashSet<>());

    getDevicesOfType(SimpleLight.class).stream()
        .filter(light -> !notLights.contains(light.getName()))
        .filter(Device::isReachable)
        .forEach(light -> {
          // see #74, if the commands are cumming to fast, then maybe lights are not correctly off
          // if this workaround helps, then this should be removed for a rate-limit (see #3)
          SleepUtil.sleep(Duration.of(25, MILLIS));

          if (light instanceof DimmableLight dimmable) {
            dimmable.setBrightness(0, Duration.of(10, SECONDS));
          } else {
            light.turnOff();
          }
        });
  }

  public boolean isAnyLightOn() {
    return getDevicesOfType(SimpleLight.class)
        .stream()
        .filter(Device::isReachable)
        .filter(light -> !notLights.contains(light.getName()))
        .anyMatch(SimpleLight::isCurrentStateIsOn);
  }

  @Transactional
  @ConsumeEvent(value = "home/animation/play", blocking = true)
  public void playAnimation(Animation animation) {
    if (runningAnimations.containsKey(animation.getId())) {
      return;
    }

    log.info("Start animation {}", animation.getId());
    var freshAnimation = animationRepository.getOne(animation.getId());
    var animationSteps = freshAnimation.materializeSteps();

    runningAnimations.put(animation.getId(), Observable.fromRunnable(() -> animationSteps
            .forEach(step -> {
              if (runningAnimations.containsKey(animation.getId())) {
                step.play(this);
              }
            }))
        .subscribeOn(RxHelper.blockingScheduler(vertx))
        .subscribe(ignore -> runningAnimations.remove(animation.getId())));
  }

  @Transactional
  @ConsumeEvent(value = "home/animation/turn-off", blocking = true)
  public void turnAnimationOff(Animation animation) {
    log.info("Stop animation {}", animation.getId());

    if (runningAnimations.containsKey(animation.getId())) {
      runningAnimations.get(animation.getId()).dispose();
      runningAnimations.remove(animation.getId());
    }

    var lights = animationRepository.getOne(animation.getId()).getLights();
    getDevicesOfType(SimpleLight.class)
        .stream()
        .filter(light -> lights.contains(light.getName()))
        .forEach(SimpleLight::turnOff);
  }

  public void activeSceneForAllGroups(String sceneName) {
    getDevicesOfType(Group.class)
        .stream()
        .flatMap(group -> group.getScenes().stream())
        .filter(scene -> scene.getName().equals(sceneName))
        .forEach(Scene::activate);
  }
}
