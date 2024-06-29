package ch.akop.homesystem.persistence.conveter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.AttributeConverter;
import lombok.NonNull;
import org.springframework.util.StringUtils;

public class ListOfStringsConverter implements AttributeConverter<List<String>, String> {

  @Nullable
  @Override
  public String convertToDatabaseColumn(@Nullable List<String> attribute) {
    return attribute == null
        ? null
        : String.join(";", attribute);
  }

  @NonNull
  @Override
  public List<String> convertToEntityAttribute(@Nullable String dbData) {
    return StringUtils.isEmpty(dbData)
        ? new ArrayList<>()
        : Arrays.stream(dbData.split(";")).toList();
  }
}
