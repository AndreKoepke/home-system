package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.states.Event;
import ch.akop.homesystem.models.states.NormalState;
import ch.akop.homesystem.models.states.SleepState;
import ch.akop.homesystem.models.states.State;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StateServiceImpl {

    @Lazy
    @Autowired
    private SleepState sleepState;

    @Lazy
    @Autowired
    private NormalState normalState;

    private final Map<Class<?>, State> states = new HashMap<>();

    private State currentState;

    @PostConstruct
    public void createStateInSomeSeconds() {
        createStates();
    }


    public void createStates() {
        this.states.put(SleepState.class, this.sleepState);
        this.states.put(NormalState.class, this.normalState);

        this.currentState = getDefaultState();
        this.currentState.entered();
    }

    public void triggerEvent(final Event event) {
        this.currentState.event(event);
    }

    public void triggerEvent(final String buttonName, final int buttonEvent) {
        this.currentState.event(buttonName, buttonEvent);
    }

    public State getDefaultState() {
        return this.states.get(NormalState.class);
    }

    public void switchState(final Class<?> toState) {
        if (this.currentState != null) {
            this.currentState.leave();
        }

        this.currentState = this.states.get(toState);

        if (this.currentState != null) {
            this.currentState.entered();
        }
    }

}
