package ch.akop.homesystem.services.impl;

import static ch.akop.homesystem.util.EventConstants.GENERAL;

import ch.akop.homesystem.models.events.Event;
import ch.akop.homesystem.persistence.model.config.UserConfig;
import ch.akop.homesystem.persistence.repository.config.UserConfigRepository;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;

@Slf4j
@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class UserService {

  public static final Duration KEEP_CHECKING_FOR = Duration.of(15, ChronoUnit.MINUTES);
  public static final int ALLOWED_FAILS = 3;

  private final Vertx vertx;
  private final ManagedExecutor executor;
  private final UserConfigRepository userConfigRepository;

  private final Subject<Map<String, Boolean>> presenceMap$ = ReplaySubject.createWithSize(1);
  private final AtomicInteger runningCheckers = new AtomicInteger();
  private ConcurrentMap<String, Boolean> presenceMap = new ConcurrentHashMap<>();

  private LocalDateTime discoverUntil;

  @PostConstruct
  @Transactional
  void setupPresenceMap() {
    presenceMap = userConfigRepository.findAll()
        .stream()
        .collect(Collectors.toConcurrentMap(
            UserConfig::getName,
            user -> user.getFailedPings() < ALLOWED_FAILS
        ));
    presenceMap$.onNext(presenceMap);
  }

  @ConsumeEvent(value = GENERAL, blocking = true)
  public void gotEvent(Event event) {
    if (event == Event.DOOR_CLOSED && runningCheckers.get() == 0) {
      log.info("Start discovering users ...");
      startCheckPresence();
    } else {
      discoverUntil = LocalDateTime.now().plus(KEEP_CHECKING_FOR);
    }
  }

  private void startCheckPresence() {
    userConfigRepository.findAll().forEach(user -> executor.runAsync(() -> updatePresence(user)));
  }

  @SneakyThrows
  private void updatePresence(UserConfig user) {
    runningCheckers.incrementAndGet();

    log.info("Checking presence for user {}", user.getName());
    do {
      if (canPingIp(user)) {
        reportUserIsAtHome(user);
      } else {
        reportUserIsNotAtHome(user);
      }
      Thread.sleep(10000);
    } while (discoverUntil.isAfter(LocalDateTime.now()));
    log.info("Finished presence check for user {}", user.getName());

    runningCheckers.decrementAndGet();
  }

  private void reportUserIsNotAtHome(UserConfig user) {
    user.increaseFailedPings();
    Observable.fromRunnable(() -> userConfigRepository.save(user)).subscribeOn(RxHelper.blockingScheduler(vertx)).subscribe();

    var userAppearsToNotBeAtHome = user.getFailedPings() > ALLOWED_FAILS;
    if (presenceMap.get(user.getName()) && userAppearsToNotBeAtHome) {
      log.info("{} is not at home anymore", user.getName());
      presenceMap.put(user.getName(), false);
      notifyPresenceMapChanged();
    }
  }

  private void reportUserIsAtHome(UserConfig user) {
    if (user.getFailedPings() == 0) {
      return;
    }
    user.setFailedPings(0);
    Observable.fromRunnable(() -> userConfigRepository.save(user)).subscribeOn(RxHelper.blockingScheduler(vertx)).subscribe();

    if (!presenceMap.get(user.getName())) {
      log.info("{} is now at home", user.getName());
      presenceMap.put(user.getName(), true);
      notifyPresenceMapChanged();
    }
  }

  private void notifyPresenceMapChanged() {
    Observable.fromRunnable(() -> presenceMap$.onNext(new HashMap<>(presenceMap)))
        .subscribeOn(RxHelper.blockingScheduler(vertx))
        .subscribe();
  }

  private boolean canPingIp(UserConfig userConfig) {
    try {
      return InetAddress.getByName(userConfig.getDeviceIp()).isReachable(3000);
    } catch (IOException ignored) {
      return false;
    }
  }

  public Flowable<Map<String, Boolean>> getPresenceMap$() {
    return presenceMap$.toFlowable(BackpressureStrategy.DROP);
  }

  public Flowable<Boolean> isAnyoneAtHome$() {
    return presenceMap$
        .distinctUntilChanged()
        .subscribeOn(RxHelper.blockingScheduler(vertx))
        .map(presenceMapUpdate -> presenceMapUpdate.values().stream().anyMatch(isAtHome -> isAtHome))
        .toFlowable(BackpressureStrategy.ERROR);
  }

  public boolean isAnyoneAtHome() {
    return presenceMap.values().stream().anyMatch(isAtHome -> isAtHome);
  }
}
