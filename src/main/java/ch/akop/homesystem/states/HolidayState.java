package ch.akop.homesystem.states;


import ch.akop.homesystem.models.events.Event;
import ch.akop.homesystem.services.activatable.Activatable;
import ch.akop.homesystem.services.activatable.SunsetReactor;
import ch.akop.homesystem.services.impl.DeviceService;
import ch.akop.homesystem.services.impl.StateService;
import ch.akop.homesystem.services.impl.TelegramMessageService;
import ch.akop.homesystem.services.impl.UserService;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;
import io.reactivex.rxjava3.core.Observable;
import lombok.RequiredArgsConstructor;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.concurrent.TimeUnit;

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


    @PostConstruct
    void registerState() {
        stateService.registerState(HolidayState.class, this);
    }

    @Override
    protected void started() {
        entered();
    }

    @Override
    public void entered() {

        messageService.sendMessageToMainChannel("Ich wünsche euch einen schönen Urlaub. Wenn ihr wieder da seid, " +
                "dann schreibt /back .");

        var durationToLightOffTime = Duration.between(ZonedDateTime.now(), getLightOffTime()).toSeconds();
        super.disposeWhenClosed(
                Observable.interval(durationToLightOffTime, ONE_DAY_AS_SECONDS, TimeUnit.SECONDS)
                        .subscribe(ignore -> deviceService.turnAllLightsOff()));

        super.disposeWhenClosed(sunsetReactor.start());
        super.disposeWhenClosed(userService.getPresenceMap$()
                .skip(10, TimeUnit.MINUTES)
                .map(ignored -> userService.isAnyoneAtHome())
                .filter(anyOneAtHome -> anyOneAtHome)
                .subscribe(ignore -> stateService.switchState(NormalState.class)));
        super.disposeWhenClosed(messageService.getMessages()
                .filter(message -> message.equals("/back"))
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
        messageService.sendMessageToMainChannel("Willkommen zurück. 👋");
        super.dispose();
    }

    @ConsumeEvent("home/general")
    public void event(Event event) {
        if (event == Event.DOOR_OPENED && stateService.getCurrentState() instanceof HolidayState) {
            messageService.sendMessageToMainChannel("Irgendwer ist grade in die Wohnung gegangen");
        }
    }
}
