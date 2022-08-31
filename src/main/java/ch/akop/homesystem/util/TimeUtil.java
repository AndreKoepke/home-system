package ch.akop.homesystem.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TimeUtil {

    public static LocalDateTime getLocalDateTimeForTodayOrTomorrow(@Nullable LocalTime time) {

        if (time == null) {
            return LocalDateTime.MAX;
        }

        var dateTime = time.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toLocalDateTime();
        if (dateTime.isAfter(LocalDateTime.now())) {
            return dateTime;
        }

        return time.atDate(LocalDate.now().plusDays(1));
    }

}
