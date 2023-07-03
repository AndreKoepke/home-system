package ch.akop.homesystem.util;

import static ch.akop.homesystem.util.Comparer.is;
import static java.math.BigDecimal.ONE;
import static java.math.BigDecimal.TEN;
import static java.math.BigDecimal.ZERO;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ComparerTest {

  @Test
  void test() {
    assertThat(is(ONE).biggerAs(ZERO)).isTrue();
    assertThat(is(ONE).biggerAs(TEN)).isFalse();
    assertThat(is(ONE).sameAs(ONE)).isTrue();
    assertThat(is(ONE).sameAs(TEN)).isFalse();
    assertThat(is(ONE).smallerThan(TEN)).isTrue();
    assertThat(is(ONE).smallerThan(ZERO)).isFalse();
  }

}
