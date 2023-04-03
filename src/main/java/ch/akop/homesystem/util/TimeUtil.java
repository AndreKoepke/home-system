package ch.akop.homesystem.util;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeUtil {

  /**
   * Calculates the text {@link LocalDateTime} for a given {@link LocalTime}. E.g. it's currently 13:22:11 and given time is "13:22", then this method returns a {@link LocalDateTime} at "13:22" of the next day.
   *
   * @param time The LocalDate
   * @return A {@link LocalDateTime}, that represents the next occurrence of the given {@link LocalTime}.
   */
  public static LocalDateTime getLocalDateTimeForTodayOrTomorrow(@Nullable LocalTime time) {

    if (time == null) {
      return LocalDateTime.MAX;
    }

    var dateTime = time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toLocalDateTime();
    var difference = Duration.between(LocalDateTime.now(), dateTime);
    if (difference.compareTo(Duration.ofSeconds(10)) > 0) {
      return dateTime;
    }

    return time.atDate(LocalDate.now().plusDays(1));
  }

}
