package ch.akop.homesystem.controller;

import ch.akop.homesystem.services.ImageCreatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Slf4j
@RestController
@RequestMapping("/v1/images")
@RequiredArgsConstructor
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class ImageController {

    private final ImageCreatorService imageCreatorService;

    // @Cacheable("dailyImage")
    @GetMapping("daily.jpg")
    public ResponseEntity<StreamingResponseBody> getDailyImage() {
        var image = imageCreatorService.getLastImage();
        imageCreatorService.increaseDownloadCounter();

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.of(12, ChronoUnit.HOURS)))
                .eTag(image.getCreated().toString())
                .contentType(MediaType.IMAGE_JPEG)
                .headers(header -> header.add("prompt", image.getPrompt()))
                .body(imageCreatorService::writeLastImageToStream);

    }


}
