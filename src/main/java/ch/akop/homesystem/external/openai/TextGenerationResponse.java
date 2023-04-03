package ch.akop.homesystem.external.openai;

import java.util.List;
import lombok.Data;

@Data
public class TextGenerationResponse {

  private List<ResponseData> choices;

  @Data
  public static class ResponseData {

    private String text;
  }

}
