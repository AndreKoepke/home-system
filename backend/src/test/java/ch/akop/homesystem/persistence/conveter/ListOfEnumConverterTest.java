package ch.akop.homesystem.persistence.conveter;

import static org.assertj.core.api.Assertions.assertThat;

import ch.akop.homesystem.models.CompassDirection;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListOfEnumConverterTest {

  ListOfEnumConverter testee = new ListOfEnumConverter();

  @Test
  void stringToEnumWorks() {
    var list = List.of(CompassDirection.EAST, CompassDirection.NORTH);
    var asString = testee.convertToDatabaseColumn(list);
    assertThat(asString).isEqualTo("EAST,NORTH");
    var backAsList = testee.convertToEntityAttribute(asString);
    assertThat(backAsList).containsExactlyInAnyOrder(CompassDirection.NORTH, CompassDirection.EAST);
  }

}
