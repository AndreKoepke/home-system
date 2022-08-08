package ch.akop.homesystem.services;

import ch.akop.homesystem.config.properties.HomeSystemProperties;
import io.reactivex.rxjava3.core.Flowable;

import java.util.Map;

public interface UserService {

    /**
     * Give the userService a hint, to recheck the presence.
     * As example: When the houseDoor was opened.
     */
    void hintCheckPresence();

    Flowable<Map<HomeSystemProperties.User, Boolean>> getPresenceMap$();

    void messageToUser(String name, String message);

    void devMessage(String message);
}
