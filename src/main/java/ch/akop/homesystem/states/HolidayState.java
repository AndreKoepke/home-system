package ch.akop.homesystem.states;


import ch.akop.homesystem.services.DeviceService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.UserService;
import ch.akop.homesystem.services.activatable.Activatable;
import ch.akop.homesystem.services.activatable.SunsetReactor;
import ch.akop.homesystem.services.impl.StateServiceImpl;
import io.reactivex.rxjava3.core.Observable;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.concurrent.TimeUnit;

@Lazy
@Service
@RequiredArgsConstructor
public class HolidayState extends Activatable implements State {

    public static final int ONE_DAY_AS_SECONDS = 60 * 60 * 24;
    private static final LocalTime LIGHT_OFF_TIME = LocalTime.of(22, 30);

    private final MessageService messageService;
    private final StateServiceImpl stateService;
    private final SunsetReactor sunsetReactor;
    private final DeviceService deviceService;
    private final UserService userService;


    @Override
    protected void started() {
        entered();
    }

    @Override
    public void entered() {

        this.messageService.sendMessageToMainChannel("Ich wünsche euch einen schönen Urlaub. Wenn ihr wieder da seid, " +
                "dann schreibt /back .");

        var durationToLightOffTime = Duration.between(ZonedDateTime.now(), getLightOffTime()).toSeconds();
        super.disposeWhenClosed(
                Observable.interval(durationToLightOffTime, ONE_DAY_AS_SECONDS, TimeUnit.SECONDS)
                        .subscribe(ignore -> this.deviceService.turnAllLightsOff()));

        super.disposeWhenClosed(this.sunsetReactor.start());
        super.disposeWhenClosed(this.userService.getPresenceMap$()
                .skip(10, TimeUnit.MINUTES)
                .map(ignored -> userService.isAnyoneAtHome())
                .filter(anyOneAtHome -> anyOneAtHome)
                .subscribe(ignore -> this.stateService.switchState(NormalState.class)));
        super.disposeWhenClosed(this.messageService.getMessages()
                .filter(message -> message.equals("/back"))
                .subscribe(ignore -> this.stateService.switchState(NormalState.class)));
    }

    private ZonedDateTime getLightOffTime() {

        if (LocalTime.now().isBefore(LIGHT_OFF_TIME)) {
            return ZonedDateTime.of(LocalDate.now(), LIGHT_OFF_TIME, ZoneId.systemDefault());
        }

        return ZonedDateTime.of(LocalDate.now().plusDays(1), LIGHT_OFF_TIME, ZoneId.systemDefault());
    }


    @Override
    public void leave() {
        this.messageService.sendMessageToMainChannel("Willkommen zurück. 👋");
        super.dispose();
    }

    @EventListener
    public void event(Event event) {
        if (event == Event.DOOR_OPENED && this.stateService.getCurrentState() instanceof HolidayState) {
            this.messageService.sendMessageToMainChannel("Irgendwer ist grade in die Wohnung gegangen");
        }
    }
}
