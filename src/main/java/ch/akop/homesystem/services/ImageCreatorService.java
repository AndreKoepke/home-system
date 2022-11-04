package ch.akop.homesystem.services;

import ch.akop.homesystem.persistence.model.ImageOfOpenAI;

public interface ImageCreatorService {
    void generateAndSendDailyImage();

    ImageOfOpenAI getLastImage();

}
