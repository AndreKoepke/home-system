package ch.akop.homesystem.services;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.services.impl.WeatherServiceImpl;
import ch.akop.weathercloud.Weather;
import io.reactivex.rxjava3.core.Flowable;

public interface WeatherService {

    Flowable<Weather> getWeather();

    Flowable<WeatherServiceImpl.CurrentAndPreviousWeather> getCurrentAndPreviousWeather();

    boolean isActive();

    CompassDirection getCurrentSunDirection();

}
