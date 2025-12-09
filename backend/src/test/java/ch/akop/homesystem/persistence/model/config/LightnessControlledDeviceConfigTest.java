package ch.akop.homesystem.persistence.model.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockStatic;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LightnessControlledDeviceConfigTest {

  private static final LocalTime TWELVE_O_CLOCK = LocalTime.of(12, 0);
  private static final LocalTime ELEVEN_O_CLOCK = LocalTime.of(11, 0);
  private static final LocalTime SEVEN_O_CLOCK = LocalTime.of(7, 0);
  private static final LocalTime SIX_O_CLOCK = LocalTime.of(6, 0);


  private static List<Arguments> isTimeOkForBeingOnArgs() {
    return List.of(
        Arguments.of("positive window, current time is outside", SEVEN_O_CLOCK, TWELVE_O_CLOCK, SIX_O_CLOCK, true),
        Arguments.of("positive window, current time is inside", SIX_O_CLOCK, TWELVE_O_CLOCK, SEVEN_O_CLOCK, false),
        Arguments.of("negative window, current time is outside", TWELVE_O_CLOCK, SIX_O_CLOCK, SEVEN_O_CLOCK, true),
        Arguments.of("negative window, current time is inside after 24:00 ", TWELVE_O_CLOCK, SEVEN_O_CLOCK, SIX_O_CLOCK, false),
        Arguments.of("negative window, current time is inside before 24:00 ", ELEVEN_O_CLOCK, SEVEN_O_CLOCK, TWELVE_O_CLOCK, false)
    );
  }

  @ParameterizedTest
  @MethodSource("isTimeOkForBeingOnArgs")
  void isTimeOkForBeingOn(String message, LocalTime keepOffFrom, LocalTime keepOffTo, LocalTime currentTime, boolean expectedResult) {
    try (var localTimeMock = mockStatic(LocalTime.class)) {
      var testee = new LightnessControlledDeviceConfig()
          .setKeepOffFrom(keepOffFrom)
          .setKeepOffTo(keepOffTo);

      localTimeMock
          .when(LocalTime::now)
          .thenReturn(currentTime);

      assertEquals(expectedResult, testee.isTimeOkForBeingOn(), message);
    }
  }
}
