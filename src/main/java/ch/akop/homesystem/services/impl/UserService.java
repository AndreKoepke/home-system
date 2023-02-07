package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.events.Event;
import ch.akop.homesystem.persistence.model.config.UserConfig;
import ch.akop.homesystem.persistence.repository.config.UserConfigRepository;
import ch.akop.homesystem.util.SleepUtil;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class UserService {

    private final UserConfigRepository userConfigRepository;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final Subject<Map<String, Boolean>> presenceMap$ = ReplaySubject.createWithSize(1);
    private Map<String, Boolean> presenceMap = new HashMap<>();


    public void hintCheckPresence() {
        executorService.submit(this::checkPresenceUntilChangedWithin);
    }

    @ConsumeEvent("home/general")
    public void gotEvent(Event event) {
        if (event == Event.DOOR_CLOSED) {
            hintCheckPresence();
        }
    }

    @SneakyThrows
    private void checkPresenceUntilChangedWithin() {
        var startedAt = LocalDateTime.now();
        var stopAt = startedAt.plus(Duration.of(5, ChronoUnit.MINUTES));

        do {
            SleepUtil.sleep(Duration.of(1, ChronoUnit.MINUTES));
            updatePresence();
        } while (LocalDateTime.now().isBefore(stopAt));
    }


    private void updatePresence() {
        var newPresenceMap = userConfigRepository.findAll().stream()
                .collect(Collectors.toMap(
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
            return InetAddress.getByName(userConfig.getDeviceIp()).isReachable(5000);
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
