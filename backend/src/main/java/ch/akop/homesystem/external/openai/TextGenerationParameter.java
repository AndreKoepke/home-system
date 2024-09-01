package ch.akop.homesystem.external.openai;

import java.util.List;
import lombok.Data;

@Data
public class TextGenerationParameter {

  private List<Message> messages;
  private String model = "text-davinci-003";

  public record Message(
      String role,
      String content
  ) {

  }
}
