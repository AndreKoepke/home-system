package ch.akop.homesystem.external.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Data
public class ImageRequest {

  private String prompt;
  private int n;
  private Size size;

  @JsonProperty("response_format")
  private ResponseFormat responseFormat;

  @RequiredArgsConstructor
  public enum Size {
    SMALL("256x256"),
    MEDIUM("512x512"),
    BIG("1024x1024");

    @Getter
    @JsonValue
    private final String text;
  }

  @RequiredArgsConstructor
  public enum ResponseFormat {
    URL("url"),
    B64_JSON("b64_json");

    @Getter
    @JsonValue
    private final String text;

  }
}
