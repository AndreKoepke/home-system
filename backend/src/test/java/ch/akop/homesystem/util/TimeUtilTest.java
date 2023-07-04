package ch.akop.homesystem.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Period;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TimeUtilTest {


  private static Stream<Arguments> datesWithExpected() {
    return Stream.of(
        Arguments.of("5 Tage", Period.ofDays(5)),
        Arguments.of("1 woche", Period.ofDays(7)),
        Arguments.of("3 monate", Period.ofMonths(3))
    );
  }

  @ParameterizedTest
  @MethodSource("datesWithExpected")
  void testParseDurations(String text, Period expected) {
    assertThat(TimeUtil.parseGermanDuration(text)).isEqualTo(expected);
  }
}
