package ch.akop.homesystem.persistence.conveter;

import ch.akop.homesystem.models.CompassDirection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.AttributeConverter;
import lombok.NonNull;
import org.springframework.util.StringUtils;

public class ListOfEnumConverter implements AttributeConverter<List<CompassDirection>, String> {

  @Nullable
  @Override
  public String convertToDatabaseColumn(@Nullable List<CompassDirection> attribute) {
    return attribute == null
        ? null
        : attribute.stream().map(CompassDirection::name).collect(Collectors.joining(","));
  }

  @NonNull
  @Override
  public List<CompassDirection> convertToEntityAttribute(@Nullable String dbData) {
    return StringUtils.isEmpty(dbData)
        ? new ArrayList<>()
        : Arrays.stream(dbData.split(",")).map(CompassDirection::valueOf).toList();
  }
}
