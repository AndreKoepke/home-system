package ch.akop.homesystem.config;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@ConfigurationPropertiesBinding
public class LocalTimeConverter implements Converter<String, LocalTime> {

    @Override
    public LocalTime convert(@NotNull String from) {
        return LocalTime.parse(from, DateTimeFormatter.ofPattern("HH:mm"));
    }

}
