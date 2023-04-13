package ch.akop.homesystem.external.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class TextGenerationParameter {

  private String prompt;
  private String model = "text-davinci-003";
  private double temperature = 1.1;

  @JsonProperty("max_tokens")
  private int maxTokens = 100;

}
