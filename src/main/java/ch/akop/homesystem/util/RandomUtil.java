package ch.akop.homesystem.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.List;
import java.util.Random;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RandomUtil {

    public static final Random RANDOM = new Random();

    public static <T> T pickRandomElement(@NonNull List<T> elements) {
        return elements.get(RANDOM.nextInt(elements.size()));
    }

}
