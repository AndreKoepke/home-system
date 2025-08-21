package ch.akop.homesystem.controller.dtos;

import static ch.akop.weathercloud.light.LightUnit.KILO_LUX;
import static ch.akop.weathercloud.rain.RainUnit.MILLIMETER_PER_HOUR;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;
import static ch.akop.weathercloud.wind.WindSpeedUnit.KILOMETERS_PER_SECOND;

import ch.akop.weathercloud.Weather;
import java.math.BigDecimal;
import java.time.ZonedDateTime;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WeatherDto implements Identable {

    private final ZonedDateTime recordedAt;
    private final ValueAndUnitDto wind;
    private final ValueAndUnitDto rain;
    private final ValueAndUnitDto outerLight;
    private final ValueAndUnitDto outerTemperature;

  public String getId() {
    return recordedAt.toString();
  }

    @Data
    private static class ValueAndUnitDto {
        private final BigDecimal value;
        private final String unit;
    }

    public static WeatherDto from(Weather weather) {
        return WeatherDto.builder()
                .recordedAt(weather.getRecordedAt())
                .wind(new ValueAndUnitDto(weather.getWind().getAs(KILOMETERS_PER_SECOND), KILOMETERS_PER_SECOND.getTextSuffix()))
                .rain(new ValueAndUnitDto(weather.getRain().getAs(MILLIMETER_PER_HOUR), MILLIMETER_PER_HOUR.getTextSuffix()))
                .outerLight(new ValueAndUnitDto(weather.getLight().getAs(KILO_LUX), KILO_LUX.getTextSuffix()))
                .outerTemperature(new ValueAndUnitDto(weather.getOuterTemperatur().getAs(DEGREE), DEGREE.getTextSuffix()))
                .build();
    }
}
