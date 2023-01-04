package ch.akop.homesystem.services;

import ch.akop.homesystem.models.CompassDirection;
import ch.akop.homesystem.services.impl.WeatherServiceImpl;
import ch.akop.weathercloud.Weather;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.ReplaySubject;

public interface WeatherService {

    ReplaySubject<Weather> getWeather();

    Observable<WeatherServiceImpl.CurrentAndPreviousWeather> getCurrentAndPreviousWeather();

    boolean isActive();

    CompassDirection getCurrentSunDirection();

}
