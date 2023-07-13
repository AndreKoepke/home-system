package ch.akop.homesystem.util;

import io.quarkus.runtime.util.StringUtil;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeUtil {

  public static final Pattern NUMERIC_PATTERN = Pattern.compile("\\d+");
  public static final Pattern TEXT_PATTERN = Pattern.compile("[a-zA-Z]+");

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

  @NonNull
  public static Period parseGermanDuration(@Nullable String text) {
    if (StringUtil.isNullOrEmpty(text)) {
      return Period.ZERO;
    }

    var numericPart = findFirst(NUMERIC_PATTERN, text).orElseThrow(() -> new IllegalArgumentException("No number found"));
    var textPart = findFirst(TEXT_PATTERN, text).orElseThrow(() -> new IllegalArgumentException("Not text for unit found"));
    var unit = GermanTimeUnits.find(textPart).orElseThrow(() -> new IllegalArgumentException("No matching unit"));

    return switch (unit.unit) {
      case DAYS -> Period.ofDays(Integer.parseInt(numericPart));
      case WEEKS -> Period.ofWeeks(Integer.parseInt(numericPart));
      case MONTHS -> Period.ofMonths(Integer.parseInt(numericPart));
      default -> throw new IllegalArgumentException("Unit " + unit.unit + " is not implemented");
    };
  }

  private static Optional<String> findFirst(Pattern pattern, String text) {
    var matcher = pattern.matcher(text);
    if (matcher.find()) {
      return Optional.of(matcher.group(0));
    }

    return Optional.empty();
  }


  @RequiredArgsConstructor
  private enum GermanTimeUnits {
    DAYS(ChronoUnit.DAYS, List.of("tage", "tag")),
    WEEKS(ChronoUnit.WEEKS, List.of("wochen", "woche")),
    MONTHS(ChronoUnit.MONTHS, List.of("monate", "monat"));

    private final ChronoUnit unit;
    private final List<String> synonyms;

    public static Optional<GermanTimeUnits> find(String text) {
      return Arrays.stream(GermanTimeUnits.values())
          .filter(unit -> unit.synonyms.contains(text.toLowerCase()))
          .findFirst();
    }
  }
}
