package ch.akop.homesystem.persistence.conveter;

import ch.akop.homesystem.models.color.Color;
import io.quarkus.runtime.util.StringUtil;
import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class ColorConverter implements AttributeConverter<Color, String> {

  @Override
  public String convertToDatabaseColumn(Color attribute) {

    if (attribute == null) {
      return null;
    }

    return "#%02X%02X%02X".formatted(
        attribute.getR(),
        attribute.getG(),
        attribute.getB());
  }

  @Override
  public Color convertToEntityAttribute(String dbData) {

    if (StringUtil.isNullOrEmpty(dbData)) {
      return null;
    }

    return new Color()
        .setR(Integer.valueOf(dbData.substring(1, 3), 16))
        .setG(Integer.valueOf(dbData.substring(3, 5), 16))
        .setB(Integer.valueOf(dbData.substring(5, 7), 16));
  }
}
