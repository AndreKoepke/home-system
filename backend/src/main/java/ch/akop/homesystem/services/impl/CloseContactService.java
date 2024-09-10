package ch.akop.homesystem.services.impl;

import static ch.akop.homesystem.util.EventConstants.CLOSE_CONTACT;

import ch.akop.homesystem.models.devices.sensor.CloseContactState;
import ch.akop.homesystem.models.events.CloseContactEvent;
import ch.akop.homesystem.persistence.repository.config.CloseContactConfigRepository;
import ch.akop.homesystem.states.SleepState;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import javax.enterprise.context.ApplicationScoped;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@ApplicationScoped
public class CloseContactService {

  private final CloseContactConfigRepository closeContactConfigRepository;
  private final TelegramMessageService telegramMessageService;
  private final EventBus eventBus;
  private final StateService stateService;

  @ConsumeEvent(value = CLOSE_CONTACT, blocking = true)
  @Transactional
  public void receiveCloseContactOpen(CloseContactEvent closeContactEvent) {

    if (closeContactEvent.getNewState().equals(CloseContactState.CLOSED)
        || !stateService.isState(SleepState.class)) {
      return;
    }

    var configOpt = closeContactConfigRepository.findById(closeContactEvent.getName());

    if (configOpt.isEmpty()) {
      return;
    }

    var config = configOpt.get();

    if (!StringUtils.isEmpty(config.getMessageWhenTrigger())) {
      telegramMessageService.sendFunnyMessageToMainChannel(config.getMessageWhenTrigger());
    }

    if (config.getAnimationWhenTrigger() != null) {
      eventBus.publish("home/animation/play", config.getAnimationWhenTrigger());
    }

  }

}
