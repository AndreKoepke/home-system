package ch.akop.homesystem.external.openai;

import java.util.List;
import lombok.Data;

@Data
public class ImageGenerationResponse {

  private List<ResponseData> data;

  @Data
  public static class ResponseData {

    private String url;
    private String b64_json;
  }

}
