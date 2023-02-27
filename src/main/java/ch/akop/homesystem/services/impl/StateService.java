package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.states.State;
import io.quarkus.runtime.Startup;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.enterprise.context.ApplicationScoped;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Startup
@ApplicationScoped
public class StateService {


    private final Map<Class<?>, State> states = new HashMap<>();

    @Getter
    private State currentState;


    public void registerState(Class<?> clazz, State state) {
        states.put(clazz, state);
    }

    public boolean isState(Class<?> state) {
        return currentState != null && state.isAssignableFrom(currentState.getClass());
    }

    public void switchState(Class<?> toState) {

        if (isState(currentState.getClass())) {
            // NOP
            return;
        }

        if (currentState != null) {
            try {
                currentState.leave();
            } catch (Exception e) {
                log.error("There was an exception in the 'leave'-method of {}",
                        currentState.getClass().getSimpleName(),
                        e);
            }
        }

        currentState = states.get(toState);

        if (currentState != null) {
            try {
                currentState.entered();
            } catch (Exception e) {
                log.error("There was an exception in the 'entered'-method of {}",
                        currentState.getClass().getSimpleName(),
                        e);
            }
        }
    }

}
