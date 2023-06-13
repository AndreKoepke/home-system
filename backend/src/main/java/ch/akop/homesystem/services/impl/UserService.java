package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.events.Event;
import ch.akop.homesystem.persistence.model.config.UserConfig;
import ch.akop.homesystem.persistence.repository.config.UserConfigRepository;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.util.StringUtil;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import io.vertx.core.Vertx;
import io.vertx.rxjava3.RxHelper;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
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
  public static final Duration KEEP_CHECKING_FOR = Duration.of(15, ChronoUnit.MINUTES);
  public static final int ALLOWED_FAILS = 3;

  private final Vertx vertx;
  private final ManagedExecutor executor;
  private final UserConfigRepository userConfigRepository;
  private final TelegramMessageService telegramMessageService;

  private final Subject<Map<String, Boolean>> presenceMap$ = ReplaySubject.createWithSize(1);
  private ConcurrentMap<String, Boolean> presenceMap = new ConcurrentHashMap<>();

  private Disposable checkPresenceSubscription;
  private Scheduler rxScheduler;

  void onStart(@Observes StartupEvent ev) {
    rxScheduler = RxHelper.blockingScheduler(vertx);
  }

  @PostConstruct
  void listenForRecheckMessage() {
    // runs forever, no need for returning anything
    //noinspection ResultOfMethodCallIgnored
    telegramMessageService.getMessages()
        .filter(message -> message.startsWith("/anyoneHome"))
        .subscribe(message -> gotEvent(Event.DOOR_CLOSED));
  }

  @Transactional
  @ConsumeEvent(value = "home/general", blocking = true)
  public void gotEvent(Event event) {
    if (event == Event.DOOR_CLOSED && (checkPresenceSubscription == null || checkPresenceSubscription.isDisposed())) {
      log.info("Start discovering users ...");
      vertx.setTimer(DELAY.toMillis(), timerId -> executor.runAsync(() -> checkPresence(LocalDateTime.now().plus(KEEP_CHECKING_FOR))));
    }
  }

  private void checkPresence(LocalDateTime discoverUntil) {
    checkPresenceSubscription = Observable.fromCallable(() -> {
          updatePresence();
          return LocalDateTime.now().isAfter(discoverUntil);
        })
        .subscribeOn(rxScheduler)
        .singleOrError()
        .subscribe(finished -> {
          if (!finished) {
            vertx.setTimer(DELAY.toMillis(), timerId -> executor.runAsync(() -> checkPresence(discoverUntil)));
          }
        });
  }

  private void updatePresence() {
    var newPresenceMap = userConfigRepository.findAll()
        .parallelStream()
        .filter(user -> !StringUtil.isNullOrEmpty(user.getDeviceIp()))
        .map(user -> user.setFailedPings(canPingIp(user) ? 0 : user.getFailedPings() + 1))
        .peek(user -> executor.runAsync(() -> userConfigRepository.save(user)))
        .collect(Collectors.toConcurrentMap(
            UserConfig::getName,
            user -> user.getFailedPings() < ALLOWED_FAILS
        ));

    var hasChanges = !newPresenceMap.equals(presenceMap);
    if (hasChanges) {
      presenceMap = newPresenceMap;
      presenceMap$.onNext(newPresenceMap);
    }
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
        .map(presenceMapUpdate -> presenceMapUpdate.values().stream().anyMatch(isAtHome -> isAtHome))
        .toFlowable(BackpressureStrategy.DROP);
  }

  public boolean isAnyoneAtHome() {
    return presenceMap.values().stream().anyMatch(isAtHome -> isAtHome);
  }
}
