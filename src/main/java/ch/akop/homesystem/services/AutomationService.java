package ch.akop.homesystem.services;

import ch.akop.homesystem.models.deconz.websocket.WebSocketUpdate;

public interface AutomationService {

    void discoverNewDevices();

}
