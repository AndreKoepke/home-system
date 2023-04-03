package ch.akop.homesystem.persistence.conveter;

import static org.assertj.core.api.Assertions.assertThat;

import ch.akop.homesystem.models.color.Color;
import org.junit.jupiter.api.Test;

class ColorConverterTest {

  private final ColorConverter testee = new ColorConverter();

  @Test
  void test_toDb() {
    assertThat(testee.convertToDatabaseColumn(Color.BLUE()))
        .isEqualTo("#0000FF");
  }

  @Test
  void test_fromDb() {
    assertThat(testee.convertToEntityAttribute("#0000FF"))
        .isEqualTo(new Color().setB(255));
  }
}
