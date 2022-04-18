package ch.akop.homesystem.message;

import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.Weather;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;

@Service
@RequiredArgsConstructor
public class WeatherPoster extends Activatable {

    private static final BigDecimal THRESHOLD_FOR_POST_NEW_TEMP = BigDecimal.valueOf(5);

    private final MessageService messageService;
    private final WeatherService weatherService;

    private Weather lastPosted;

    @Override
    protected void started() {
        super.disposeWhenClosed(this.weatherService.getWeather()
                .doOnNext(this::postWeatherWhenNecessary)
                .subscribe());
    }

    private void postWeatherWhenNecessary(final Weather weather) {
        if (shouldPostWeather(weather)) {
            tellWeather(weather);
        }
    }

    private boolean shouldPostWeather(final Weather newWeather) {

        if (this.lastPosted == null) {
            return false;
        }

        final var oldTemp = this.lastPosted.getOuterTemperatur().getAs(DEGREE);
        final var newTemp = newWeather.getOuterTemperatur().getAs(DEGREE);

        return oldTemp.subtract(newTemp).abs().compareTo(THRESHOLD_FOR_POST_NEW_TEMP) >= 1;
    }

    private void tellWeather(final Weather newWeather) {
        this.messageService.sendMessageToMainChannel("Es sind grade %s."
                .formatted(newWeather.getOuterTemperatur()));

        this.lastPosted = newWeather;
    }


}
