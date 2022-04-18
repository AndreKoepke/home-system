package ch.akop.homesystem.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.time.Duration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SleepUtil {

    @SneakyThrows
    public static void sleep(final Duration duration) {
        Thread.sleep(duration.toMillis());
    }

}
