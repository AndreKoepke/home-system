package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.states.State;
import io.quarkus.runtime.Startup;
import lombok.Getter;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@Startup
@ApplicationScoped
public class StateService {


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
