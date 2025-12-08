package ch.akop.homesystem.states;


import static ch.akop.homesystem.util.EventConstants.GENERAL;

import ch.akop.homesystem.models.events.Event;
import ch.akop.homesystem.services.activatable.Activatable;
import ch.akop.homesystem.services.activatable.SunsetReactor;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.services.impl.StateService;
import ch.akop.homesystem.services.impl.TelegramMessageService;
import ch.akop.homesystem.services.impl.UserService;
import io.quarkus.runtime.Startup;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.Observable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;

@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class HolidayState extends Activatable implements State {

  public static final int ONE_DAY_AS_SECONDS = 60 * 60 * 24;
  private static final LocalTime LIGHT_OFF_TIME = LocalTime.of(22, 30);

  private final TelegramMessageService messageService;
  private final StateService stateService;
  private final SunsetReactor sunsetReactor;
  private final DeviceService deviceService;
  private final UserService userService;

  void registerState(@Observes StartupEvent startupEvent) {
    stateService.registerState(HolidayState.class, this);
  }

  @Override
  protected void started() {
    entered(true);
  }

  @Override
  public void entered(boolean quiet) {

    if (!quiet) {
      messageService.sendMessageToMainChannel("Ich wünsche euch einen schönen Urlaub. Wenn ihr wieder da seid, " +
          "dann schreibt /back .");
    }

    var durationToLightOffTime = Duration.between(ZonedDateTime.now(), getLightOffTime()).toSeconds();
    disposeWhenClosed(
        Observable.interval(durationToLightOffTime, ONE_DAY_AS_SECONDS, TimeUnit.SECONDS)
            .subscribe(ignore -> deviceService.turnAllLightsOff()));

    disposeWhenClosed(sunsetReactor.start());
    disposeWhenClosed(userService.getPresenceMap$()
        .skip(10, TimeUnit.MINUTES)
        .map(ignored -> userService.isAnyoneAtHome())
        .filter(anyOneAtHome -> anyOneAtHome)
        .subscribe(ignore -> stateService.switchState(NormalState.class)));

    disposeWhenClosed(messageService.waitForMessageOnce("back")
        .subscribe(ignore -> stateService.switchState(NormalState.class)));
  }

  private ZonedDateTime getLightOffTime() {

    if (LocalTime.now().isBefore(LIGHT_OFF_TIME)) {
      return ZonedDateTime.of(LocalDate.now(), LIGHT_OFF_TIME, ZoneId.systemDefault());
    }

    return ZonedDateTime.of(LocalDate.now().plusDays(1), LIGHT_OFF_TIME, ZoneId.systemDefault());
  }


  @Override
  public void leave() {
    messageService.sendFunnyMessageToMainChannel("Willkommen zurück. 👋");
    super.dispose();
  }

  @ConsumeEvent(value = GENERAL, blocking = true)
  public void event(Event event) {
    if (event == Event.DOOR_OPENED && stateService.isState(HolidayState.class)) {
      messageService.sendFunnyMessageToMainChannel("Irgendwer ist grade in die Wohnung gegangen");
    }
  }
}
