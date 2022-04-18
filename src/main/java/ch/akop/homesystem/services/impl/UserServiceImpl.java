package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.config.HomeConfig;
import ch.akop.homesystem.models.config.User;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.UserService;
import io.reactivex.rxjava3.core.BackpressureStrategy;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.InetAddress;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.concurrent.TimeUnit.SECONDS;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final HomeConfig homeConfig;
    private final MessageService messageService;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);

    private final Map<User, Boolean> presenceMap = new HashMap<>();
    private final Subject<Map<User, Boolean>> presenceMap$ = ReplaySubject.createWithSize(1);


    @Override
    public void hintCheckPresence() {
        this.executorService.submit(() -> checkPresenceUntilChangedWithin(Duration.of(10, ChronoUnit.MINUTES)));
    }


    @SneakyThrows
    private void checkPresenceUntilChangedWithin(final Duration duration) {
        var result = this.executorService.schedule(this::checkPresence, 30, SECONDS);
        long retry = 0;
        Duration retryDuration = Duration.of(1, ChronoUnit.MINUTES);

        while (!result.get() && retryDuration.compareTo(duration) < 0) {
            retryDuration = Duration.of(++retry * 2, ChronoUnit.MINUTES);
            result = this.executorService.schedule(this::checkPresence, retryDuration.toSeconds(), SECONDS);
        }
    }


    private boolean checkPresence() {
        final var shouldUpdate = new AtomicBoolean(false);
        this.homeConfig.getUsers().forEach(user -> {
            final var isUserAtHome = canPingIp(user.getDeviceIp());

            if (this.presenceMap.containsKey(user)) {
                if (!this.presenceMap.get(user).equals(isUserAtHome)) {
                    this.presenceMap.put(user, isUserAtHome);
                    shouldUpdate.set(true);
                }
            } else {
                this.presenceMap.put(user, isUserAtHome);
            }
        });


        if (shouldUpdate.get()) {
            this.presenceMap$.onNext(this.presenceMap);
        }

        return shouldUpdate.get();
    }

    private boolean canPingIp(final String ip) {
        try {
            return InetAddress.getByName(ip).isReachable(5000);
        } catch (final IOException ignored) {
            return false;
        }
    }

    @Override
    public Flowable<Map<User, Boolean>> getPresenceMap$() {
        return this.presenceMap$.toFlowable(BackpressureStrategy.DROP);
    }

    @Override
    public void messageToUser(final String name, final String message) {
        this.homeConfig.getUsers().stream()
                .filter(user -> user.getName().equalsIgnoreCase(name))
                .findFirst()
                .ifPresent(user -> this.messageService.sendMessageToUser(message, user.getTelegramId()));
    }

    @Override
    public void devMessage(final String message) {
        this.homeConfig.getUsers().stream()
                .filter(User::isDev)
                .findFirst()
                .ifPresent(user -> this.messageService.sendMessageToUser(message, user.getTelegramId()));
    }
}
