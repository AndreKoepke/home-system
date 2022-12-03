package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.states.HolidayState;
import ch.akop.homesystem.states.NormalState;
import ch.akop.homesystem.states.SleepState;
import ch.akop.homesystem.states.State;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
    
    private final Map<Class<?>, State> states = new HashMap<>();

    @Getter
    private State currentState;

    @PostConstruct
    public void createStates() {
        this.states.put(SleepState.class, this.sleepState);
        this.states.put(NormalState.class, this.normalState);
        this.states.put(HolidayState.class, this.holidayState);
    }

    @Scheduled(fixedDelay = 1000)
    public void activateDefaultState() {
        if (this.currentState == null) {
            this.currentState = getDefaultState();
            this.currentState.entered();
        }
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
