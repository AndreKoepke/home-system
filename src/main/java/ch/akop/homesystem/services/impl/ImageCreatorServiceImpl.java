package ch.akop.homesystem.services.impl;

import ch.akop.homesystem.external.mastodon.MastodonService;
import ch.akop.homesystem.external.openai.OpenAIService;
import ch.akop.homesystem.persistence.model.ImageOfOpenAI;
import ch.akop.homesystem.persistence.repository.OpenAIImageRepository;
import ch.akop.homesystem.services.ImageCreatorService;
import ch.akop.homesystem.services.MessageService;
import ch.akop.homesystem.services.WeatherService;
import ch.akop.homesystem.util.RandomUtil;
import ch.akop.weathercloud.Weather;
import io.reactivex.rxjava3.disposables.Disposable;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ch.akop.weathercloud.rain.RainUnit.MILLIMETER_PER_HOUR;
import static ch.akop.weathercloud.temperature.TemperatureUnit.DEGREE;
import static java.util.Optional.ofNullable;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageCreatorServiceImpl implements ImageCreatorService {

    public static final String CACHE_NAME = "dailyImage";
    private final OpenAIService imageService;
    private final OpenAIImageRepository imageRepository;
    private final MessageService messageService;
    private final WeatherService weatherService;
    private final MastodonService mastodonService;
    private final CacheManager cacheManager;

    private Disposable messageListener;

    @PostConstruct
    private void listenForRenewCommands() {
        messageListener = messageService.getMessages()
                .filter(message -> message.equals("/neuesBild"))
                .subscribe(message -> generateAndSendDailyImage());
    }

    @PreDestroy
    private void stopListening() {
        if (messageListener != null) {
            messageListener.dispose();
        }
    }

    @Override
    public void generateAndSendDailyImage() {
        var prompt = generatePrompt();
        imageService.requestImage(prompt)
                .subscribe(image -> {
                    messageService.sendImageToMainChannel(image, prompt);
                    imageRepository.save(new ImageOfOpenAI().setPrompt(prompt).setImage(image));
                    ofNullable(cacheManager.getCache(CACHE_NAME)).ifPresent(Cache::clear);

                    mastodonService.publishImage(("Generated image for: \"%s\"\n#openai #dall·e")
                            .formatted(prompt), image);
                });
    }

    @Override
    @Transactional
    @Cacheable(CACHE_NAME)
    public ImageOfOpenAI getLastImage() {
        return imageRepository.findFirstByOrderByCreatedDesc()
                .map(image -> Hibernate.unproxy(image, ImageOfOpenAI.class))
                .orElseThrow(() -> new NoSuchElementException("There are no images right now."));
    }

    @Override
    @SneakyThrows
    public void writeLastImageToStream(OutputStream outputStream) {
        outputStream.write(getLastImage().getImage());
    }

    @Override
    @Async
    @Transactional
    public void increaseDownloadCounter(LocalDateTime imageThatWasCreatedAt) {
        imageRepository.increaseDownloadCounter(imageThatWasCreatedAt);
    }

    private String generatePrompt() {
        var atTheBeginning = List.of(
                "A swiss house in the mountains with a lake",
                "A train passing wonderful mountains",
                "A blue Ford Kuga MK 2 on the highway",
                "Hamburg",
                "Switzerland",
                "The ocean",
                "Jungfrau-Joch",
                "Space Shuttle",
                "A lake mirroring Mountains",
                "The sky",
                "In a retro bar");

        var inTheMiddle = weatherService.getWeather()
                .take(1)
                .map(this::extractTextFromWeather)
                .blockingFirst();

        var atTheEnd = List.of(
                "as an oil painting",
                "as a stained glass window",
                "as an abstract pencil and watercolor drawing",
                "in digital art",
                "as a realistic photo",
                "as a 3D render",
                "in Van Gogh style",
                "spray-painted on a wall",
                "as a 1960s poster",
                "");

        return Stream.of(atTheBeginning, inTheMiddle, atTheEnd)
                .filter(list -> !list.isEmpty())
                .map(RandomUtil::pickRandomElement)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }

    private List<String> extractTextFromWeather(Weather weather) {

        var possibleWeatherTexts = new ArrayList<String>();
        var isRaining = weather.getRain().isBiggerThan(0, MILLIMETER_PER_HOUR);
        var isVeryCold = weather.getOuterTemperatur().isSmallerThan(0, DEGREE);
        var isCold = weather.getOuterTemperatur().isSmallerThan(5, DEGREE);
        var isWarm = weather.getOuterTemperatur().isBiggerThan(15, DEGREE);

        if (isRaining && isCold) {
            possibleWeatherTexts.add("on a cold and rainy day");
            possibleWeatherTexts.add("with bad weather");
            possibleWeatherTexts.add("with sad feeling");
        }

        if (isRaining && isWarm) {
            possibleWeatherTexts.add("on a summer rainy day");
            possibleWeatherTexts.add("and summer thunderstorms");
        }

        if (isRaining) {
            possibleWeatherTexts.add("on a rainy day");
            possibleWeatherTexts.add("with bad weather");

        }

        if (isCold) {
            possibleWeatherTexts.add("in the winter");
            possibleWeatherTexts.add("on a cold day");
        }

        if (isVeryCold) {
            possibleWeatherTexts.add("at christmas");
            possibleWeatherTexts.add("at a snowy day");
            possibleWeatherTexts.add("and it is very cold outside");

        }

        if (isWarm) {
            possibleWeatherTexts.add("in the summer");
            possibleWeatherTexts.add("at a sunny day");
            possibleWeatherTexts.add("on a nice day");
            possibleWeatherTexts.add("and sunglasses");
        }

        return possibleWeatherTexts;
    }
}
