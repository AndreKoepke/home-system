package ch.akop.homesystem.util;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class Comparer<T> {

  private final Comparable<T> leftHand;

  public static <T> Comparer<T> is(Comparable<T> leftHand) {
    return new Comparer<>(leftHand);
  }

  public boolean biggerAs(T rightHand) {
    return leftHand.compareTo(rightHand) > 0;
  }

  public boolean sameAs(T rightHand) {
    return leftHand.compareTo(rightHand) == 0;
  }

  public boolean smallerThan(T rightHand) {
    return leftHand.compareTo(rightHand) < 0;
  }
}
