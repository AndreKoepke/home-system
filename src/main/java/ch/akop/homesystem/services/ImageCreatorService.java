package ch.akop.homesystem.services;

import ch.akop.homesystem.persistence.model.ImageOfOpenAI;

import java.io.OutputStream;

public interface ImageCreatorService {
    void generateAndSendDailyImage();

    ImageOfOpenAI getLastImage();

    void writeLastImageToStream(OutputStream outputStream);

    void increaseDownloadCounter();

}
