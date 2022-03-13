package ch.akop.homesystem.services;

import ch.akop.weathercloud.Weather;
import io.reactivex.rxjava3.subjects.Subject;

public interface WeatherService {

    Subject<Weather> getWeather();

    boolean isActive();

}
