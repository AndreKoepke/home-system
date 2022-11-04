package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.openai.OpenAIService;
import ch.akop.homesystem.persistence.model.ImageOfOpenAI;
import ch.akop.homesystem.persistence.repository.OpenAIImageRepository;
import ch.akop.homesystem.services.ImageCreatorService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.weathercloud.Weather;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.engine.jdbc.BlobProxy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.OutputStream;
import java.util.List;
import java.util.NoSuchElementException;

import static ch.akop.homesystem.util.RandomUtil.pickRandomElement;
import static ch.akop.weathercloud.rain.RainUnit.MILLIMETER_PER_HOUR;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCreatorServiceImpl implements ImageCreatorService {

    private final OpenAIService imageService;
    private final OpenAIImageRepository imageRepository;
    private final MessageService messageService;
    private final WeatherService weatherService;
    private final TransactionTemplate transactionTemplate;

    byte[] lastImage;


    @Override
    public void generateAndSendDailyImage() {
        var prompt = generatePrompt();
        imageService.requestImage(prompt)
                .subscribe(image -> {
                    messageService.sendImageToMainChannel(image, prompt);
                    imageRepository.save(new ImageOfOpenAI().setPrompt(prompt).setImage(BlobProxy.generateProxy(image)));
                    lastImage = image;
                });
    }

    @Override
    @Transactional
    public ImageOfOpenAI getLastImage() {
        return getLastImageOrThrow();
    }

    @Override
    @SneakyThrows
    public void writeLastImageToStream(OutputStream outputStream) {
        outputStream.write(getLastImageAsBytes());
    }

    public synchronized byte[] getLastImageAsBytes() {
        if (lastImage == null) {
            lastImage = transactionTemplate.execute(status -> getLastImageAsBytesFromDatabase());
        }

        return lastImage;
    }

    @SneakyThrows
    private byte[] getLastImageAsBytesFromDatabase() {
        return getLastImageOrThrow().getImage().getBinaryStream().readAllBytes();
    }

    @Override
    @Async
    public void increaseDownloadCounter() {
        imageRepository.findFirstByOrderByCreatedDesc()
                .ifPresent(image -> image.setDownloaded(image.getDownloaded() + 1));
    }

    private ImageOfOpenAI getLastImageOrThrow() {
        return imageRepository.findFirstByOrderByCreatedDesc()
                .orElseThrow(() -> new NoSuchElementException("There are no images right now."));
    }

    private String generatePrompt() {
        var atTheBeginning = List.of("A swiss house in the mountains with a lake",
                "A train passing wonderful mountains",
                "A blue Ford Kuga MK 2 on the highway");

        var inTheMiddle = weatherService.getWeather()
                .take(1)
                .map(this::extractTextFromWeather)
                .blockingFirst();

        var atTheEnd = List.of("as an oil painting",
                "as a stained glass window",
                "as an abstract pencil and watercolor drawing",
                "in digital art",
                "as a realistic photograph",
                "as a 3D render",
                "in Van Gogh style");

        return "%s %s %s".formatted(pickRandomElement(atTheBeginning), inTheMiddle, pickRandomElement(atTheEnd));
    }

    private String extractTextFromWeather(Weather weather) {

        var isRaining = weather.getRain().isBiggerThan(0, MILLIMETER_PER_HOUR);
        var isCold = weather.getOuterTemperatur().isSmallerThan(5, DEGREE);
        var isWarm = weather.getOuterTemperatur().isBiggerThan(15, DEGREE);

        if (isRaining && isCold) {
            return "on cold and rainy day";
        }

        if (isRaining && isWarm) {
            return "on a summer rainy day";
        }

        if (isRaining) {
            return "on a rainy day";
        }

        if (isCold) {
            return "in the winter";
        }

        if (isWarm) {
            return "in the summer";
        }

        return "";
    }
}
