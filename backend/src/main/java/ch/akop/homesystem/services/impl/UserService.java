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
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class UserService {

  private final ScheduledExecutorService scheduledExecutorService = new ScheduledThreadPoolExecutor(1);
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
      scheduledExecutorService.schedule(() -> checkPresence(userConfigRepository.findAll()), 10, TimeUnit.SECONDS);
    }
  }

  private void checkPresence(List<UserConfig> users) {
    updatePresence(users);

    if (LocalDateTime.now().isBefore(discoverUntil)) {
      scheduledExecutorService.schedule(() -> checkPresence(users), 15, TimeUnit.SECONDS);
    } else {
      log.info("Stop user-discovery");
    }
  }

  private void updatePresence(List<UserConfig> users) {
    var newPresenceMap = users.parallelStream()
        .filter(user -> !StringUtil.isNullOrEmpty(user.getDeviceIp()))
        .peek(userConfig -> log.info("Check presence for " + userConfig.getName()))
        .collect(Collectors.toConcurrentMap(
            UserConfig::getName,
            this::canPingIp
        ));

    var hasChanges = !newPresenceMap.equals(presenceMap);

    if (hasChanges) {
      presenceMap = newPresenceMap;
      presenceMap$.onNext(newPresenceMap);
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
