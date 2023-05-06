package ch.akop.homesystem.services.impl;

import static java.util.Optional.ofNullable;

import ch.akop.homesystem.persistence.repository.StateRepository;
import ch.akop.homesystem.states.NormalState;
import ch.akop.homesystem.states.State;
import io.quarkus.runtime.Startup;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Startup
@ApplicationScoped
@RequiredArgsConstructor
public class StateService {

  private static final String DEFAULT_STATE = NormalState.class.getSimpleName();

  private final Map<String, State> states = new HashMap<>();

  private final StateRepository stateRepository;

  @Getter
  private State currentState;


  @Transactional
  public void registerState(Class<?> clazz, State state) {
    states.put(clazz.getSimpleName(), state);
    if (currentState == null
        && ofNullable(stateRepository.getFirstByOrderByActivatedAtDesc())
        .map(ch.akop.homesystem.persistence.model.State::getClassName)
        .orElse(DEFAULT_STATE)
        .equals(clazz.getSimpleName())) {
      activateStateQuietly(clazz);
    }
  }

  public boolean isState(Class<?> state) {
    return currentState != null && state.isAssignableFrom(currentState.getClass());
  }

  public void activateStateQuietly(Class<?> clazz) {
    leaveCurrentState();

    var newState = states.get(clazz.getSimpleName());
    currentState = newState;
    newState.entered(true);
    stateRepository.save(new ch.akop.homesystem.persistence.model.State()
        .setClassName(clazz.getSimpleName()));
  }

  @Transactional
  public void switchState(Class<?> toState) {

    if (isState(toState)) {
      // NOP
      return;
    }

    var className = toState.getSimpleName();
    leaveCurrentState();

    currentState = states.get(toState.getSimpleName());
    stateRepository.save(new ch.akop.homesystem.persistence.model.State().setClassName(className));

    if (currentState != null) {
      try {
        currentState.entered(false);
      } catch (Exception e) {
        log.error("There was an exception in the 'entered'-method of {}", className, e);
      }
    }
  }

  private void leaveCurrentState() {
    if (currentState != null) {
      try {
        currentState.leave();
      } catch (Exception e) {
        log.error("There was an exception in the 'leave'-method of {}", currentState.getClass().getSimpleName(), e);
      }
    }
  }

}
