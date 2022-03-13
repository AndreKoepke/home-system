package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.states.Event;
import ch.akop.homesystem.states.NormalState;
import ch.akop.homesystem.states.SleepState;
import ch.akop.homesystem.states.State;
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

    private final Map<Class<?>, State> states = new HashMap<>();

    private State currentState;

    @PostConstruct
    public void createStateInSomeSeconds() {
        this.states.put(SleepState.class, this.sleepState);
        this.states.put(NormalState.class, this.normalState);
    }


    private void activateDefaultState() {
        if (this.currentState == null) {
            this.currentState = getDefaultState();
            this.currentState.entered();
        }
    }

    public void triggerEvent(final Event event) {
        activateDefaultState();
        this.currentState.event(event);
    }

    public void triggerEvent(final String buttonName, final int buttonEvent) {
        activateDefaultState();
        this.currentState.event(buttonName, buttonEvent);
    }

    public State getDefaultState() {
        return this.states.get(NormalState.class);
    }

    public void switchState(final Class<?> toState) {

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
