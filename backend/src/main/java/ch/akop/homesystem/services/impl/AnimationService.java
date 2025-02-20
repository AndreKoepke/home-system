package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.actor.SimpleLight;
import ch.akop.homesystem.persistence.model.animation.Animation;
import ch.akop.homesystem.persistence.model.animation.steps.Step;
import ch.akop.homesystem.persistence.repository.config.AnimationRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@ApplicationScoped
public class AnimationService {

  private final AnimationRepository animationRepository;
  private final DeviceService deviceService;
  private final Vertx vertx;
  private final Map<UUID, Disposable> runningAnimations = new ConcurrentHashMap<>();

  public List<Animation> getAllAnimations() {
    return animationRepository.findAll();
  }

  @Transactional
  @ConsumeEvent(value = "home/animation/play", blocking = true)
  public void playAnimation(UUID animationId) {
    if (runningAnimations.containsKey(animationId)) {
      return;
    }

    log.info("Start animation {}", animationId);
    var freshAnimation = animationRepository.getOne(animationId);
    var animationSteps = freshAnimation.materializeSteps();

    runningAnimations.put(animationId, Observable.fromRunnable(() -> playAllAnimation(freshAnimation, animationSteps))
        .subscribeOn(RxHelper.blockingScheduler(vertx))
        .subscribe(o -> {
        }));
  }

  private void playAllAnimation(Animation animation, List<? extends Step> steps) {
    try {
      steps.stream()
          .filter(step -> runningAnimations.containsKey(animation.getId()))
          .forEach(step -> step.play(deviceService));
    } catch (Exception e) {
      log.error("Error playing animation {}", animation.getId(), e);
    } finally {
      runningAnimations.remove(animation.getId());
    }
  }

  @Transactional
  @ConsumeEvent(value = "home/animation/turn-off", blocking = true)
  public void turnAnimationOff(UUID animationId) {
    log.info("Stop animation {}", animationId);

    if (runningAnimations.containsKey(animationId)) {
      runningAnimations.get(animationId).dispose();
      runningAnimations.remove(animationId);
    }

    var lights = animationRepository.getOne(animationId).getLights();
    deviceService.getDevicesOfType(SimpleLight.class)
        .stream()
        .filter(light -> lights.contains(light.getName()))
        .forEach(SimpleLight::turnOff);
  }
}
