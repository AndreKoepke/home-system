package ch.akop.homesystem.util;

import java.time.Duration;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SleepUtil {

  @SneakyThrows
  public static void sleep(final Duration duration) {
    Thread.sleep(duration.toMillis());
  }

}
