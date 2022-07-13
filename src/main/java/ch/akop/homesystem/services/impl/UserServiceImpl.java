package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.config.User;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.UserService;
import ch.akop.homesystem.states.Event;
import ch.akop.homesystem.util.SleepUtil;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

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
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final HomeConfig homeConfig;
    private final MessageService messageService;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private Map<User, Boolean> presenceMap = new HashMap<>();
    private final Subject<Map<User, Boolean>> presenceMap$ = ReplaySubject.createWithSize(1);


    @Override
    public void hintCheckPresence() {
        this.executorService.submit(this::checkPresenceUntilChangedWithin);
    }


    @EventListener
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
        var newPresenceMap = this.homeConfig.getUsers().stream()
                .collect(Collectors.toMap(
                        user -> user,
                        user -> canPingIp(user.getDeviceIp())
                ));

        var hasChanges = !newPresenceMap.equals(this.presenceMap);

        if (hasChanges) {
            this.presenceMap = newPresenceMap;
            this.presenceMap$.onNext(newPresenceMap);
        }
    }

    private boolean canPingIp(String ip) {
        try {
            return InetAddress.getByName(ip).isReachable(5000);
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public Flowable<Map<User, Boolean>> getPresenceMap$() {
        return this.presenceMap$.toFlowable(BackpressureStrategy.DROP);
    }

    @Override
    public void messageToUser(String name, String message) {
        this.homeConfig.getUsers().stream()
                .filter(user -> user.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresent(user -> this.messageService.sendMessageToUser(message, user.getTelegramId()));
    }

    @Override
    public void devMessage(String message) {
        this.homeConfig.getUsers().stream()
                .filter(User::isDev)
                .findFirst()
                .ifPresent(user -> this.messageService.sendMessageToUser(message, user.getTelegramId()));
    }
}
