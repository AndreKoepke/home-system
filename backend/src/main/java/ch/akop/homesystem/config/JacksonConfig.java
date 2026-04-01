package ch.akop.homesystem.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.quarkus.jackson.ObjectMapperCustomizer;
import jakarta.inject.Singleton;

@Singleton
public class JacksonConfig implements ObjectMapperCustomizer {

  @Override
  public void customize(ObjectMapper objectMapper) {
    objectMapper
        .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
  }
}
