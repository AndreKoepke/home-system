package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.states.State;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StateServiceImpl {


    private final Map<Class<?>, State> states = new HashMap<>();

    @Getter
    private State currentState;


    public void registerState(Class<?> clazz, State state) {
        states.put(clazz, state);
    }


    public void switchState(Class<?> toState) {

        if (currentState != null && toState.isAssignableFrom(currentState.getClass())) {
            // NOP
            return;
        }

        if (currentState != null) {
            currentState.leave();
        }

        currentState = states.get(toState);

        if (currentState != null) {
            currentState.entered();
        }
    }

}
