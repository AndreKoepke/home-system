package ch.akop.homesystem.services;

import ch.akop.weathercloud.Weather;
import io.reactivex.rxjava3.core.Flowable;

public interface WeatherService {

    Flowable<Weather> getWeather();

    boolean isActive();

}
