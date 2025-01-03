package ch.akop.homesystem.util;

import java.util.List;
import java.util.Random;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomUtil {

  public static final Random RANDOM = new Random();

  public static <T> T pickRandomElement(@NonNull List<T> elements) {
    return elements.get(RANDOM.nextInt(elements.size()));
  }

  public static boolean yesOrNo(double changeForYes) {
    return RANDOM.nextDouble() < changeForYes;
  }
}
