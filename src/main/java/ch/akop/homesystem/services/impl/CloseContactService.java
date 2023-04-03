package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.models.devices.sensor.CloseContactState;
import ch.akop.homesystem.models.events.CloseContactEvent;
import ch.akop.homesystem.persistence.repository.config.CloseContactConfigRepository;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CloseContactService {

  private final CloseContactConfigRepository closeContactConfigRepository;
  private final TelegramMessageService telegramMessageService;
  private final EventBus eventBus;

  @ConsumeEvent(value = "home/close-contact", blocking = true)
  @Transactional
  public void receiveCloseContactOpen(CloseContactEvent closeContactEvent) {

    if (!closeContactEvent.getNewState().equals(CloseContactState.OPENED)) {
      return;
    }

    var configOpt = closeContactConfigRepository.findById(closeContactEvent.getName());

    if (configOpt.isEmpty()) {
      return;
    }

    var config = configOpt.get();

    if (!StringUtils.isEmpty(config.getMessageWhenTrigger())) {
      telegramMessageService.sendMessageToMainChannel(config.getMessageWhenTrigger());
    }

    if (config.getAnimationWhenTrigger() != null) {
      eventBus.publish("home/animation/play", config.getAnimationWhenTrigger());
    }

  }

}
