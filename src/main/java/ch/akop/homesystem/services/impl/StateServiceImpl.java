package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.states.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StateServiceImpl {

    @Lazy
    private final SleepState sleepState;

    @Lazy
    private final NormalState normalState;

    @Lazy
    private final HolidayState holidayState;

    private final FanService fanService;
    private final Map<Class<?>, State> states = new HashMap<>();

    @Getter
    private State currentState;

    @PostConstruct
    public void createStates() {
        this.states.put(SleepState.class, this.sleepState);
        this.states.put(NormalState.class, this.normalState);
        this.states.put(HolidayState.class, this.holidayState);
    }


    private void activateDefaultState() {
        if (this.currentState == null) {
            this.currentState = getDefaultState();
            this.currentState.entered();
        }
    }

    public void triggerEvent(Event event) {
        activateDefaultState();
        this.currentState.event(event);
    }

    public void triggerEvent(String buttonName, int buttonEvent) {
        activateDefaultState();
        this.currentState.event(buttonName, buttonEvent);
        this.fanService.buttonEventHandler(buttonName, buttonEvent);
    }

    public State getDefaultState() {
        return this.states.get(NormalState.class);
    }

    public void switchState(Class<?> toState) {

        activateDefaultState();

        if (this.currentState != null && toState.isAssignableFrom(this.currentState.getClass())) {
            // NOP
            return;
        }

        if (this.currentState != null) {
            this.currentState.leave();
        }

        this.currentState = this.states.get(toState);

        if (this.currentState != null) {
            this.currentState.entered();
        }
    }

}
