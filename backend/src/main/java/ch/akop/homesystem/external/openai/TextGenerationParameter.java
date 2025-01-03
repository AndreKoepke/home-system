package ch.akop.homesystem.external.openai;

import java.util.List;
import lombok.Data;

@Data
public class TextGenerationParameter {

  private List<Message> messages;
  private String model = "gpt-4";

  public record Message(
      String role,
      String content
  ) {

  }
}
