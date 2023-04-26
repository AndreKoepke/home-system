package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.events.Event;
import ch.akop.homesystem.persistence.model.config.UserConfig;
import ch.akop.homesystem.persistence.repository.config.UserConfigRepository;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.util.StringUtil;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.vertx.mutiny.core.Vertx;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.context.ManagedExecutor;

@Slf4j
@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class UserService {

  private static final Duration DELAY = Duration.of(15, ChronoUnit.SECONDS);

  private final Vertx vertx;
  private final ManagedExecutor executor;
  private final Subject<Map<String, Boolean>> presenceMap$ = ReplaySubject.createWithSize(1);

  private final UserConfigRepository userConfigRepository;

  private ConcurrentMap<String, Boolean> presenceMap = new ConcurrentHashMap<>();
  private LocalDateTime discoverUntil;


  @Transactional
  @ConsumeEvent(value = "home/general", blocking = true)
  public void gotEvent(Event event) {
    if (event == Event.DOOR_CLOSED) {
      log.info("Start discovering users ...");
      discoverUntil = LocalDateTime.now().plus(Duration.of(15, ChronoUnit.MINUTES));
      var users = userConfigRepository.findAll();
      vertx.setTimer(DELAY.toMillis(), timerId -> checkPresence(users));
    }
  }

  private void checkPresence(List<UserConfig> users) {
    updatePresence(users);

    if (LocalDateTime.now().isBefore(discoverUntil)) {
      vertx.setTimer(DELAY.toMillis(), timerId -> checkPresence(users));
    } else {
      log.info("Stop user-discovery");
    }
  }

  private void updatePresence(List<UserConfig> users) {
    var newPresenceMap = users.stream()
        .filter(user -> !StringUtil.isNullOrEmpty(user.getDeviceIp()))
        .collect(Collectors.toConcurrentMap(
            UserConfig::getName,
            this::canPingIp
        ));

    var hasChanges = !newPresenceMap.equals(presenceMap);

    if (hasChanges) {
      presenceMap = newPresenceMap;
      executor.runAsync(() -> presenceMap$.onNext(newPresenceMap));
    }
  }

  private boolean canPingIp(UserConfig userConfig) {
    try {
      return InetAddress.getByName(userConfig.getDeviceIp()).isReachable(1500);
    } catch (IOException ignored) {
      return false;
    }
  }

  public Flowable<Map<String, Boolean>> getPresenceMap$() {
    return presenceMap$.toFlowable(BackpressureStrategy.DROP);
  }

  public Flowable<Boolean> isAnyoneAtHome$() {
    return presenceMap$
        .map(presenceMap -> presenceMap.values().stream().anyMatch(isAtHome -> isAtHome))
        .toFlowable(BackpressureStrategy.DROP);
  }

  public boolean isAnyoneAtHome() {
    return presenceMap.values().stream().anyMatch(isAtHome -> isAtHome);
  }
}
