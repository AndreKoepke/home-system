package ch.akop.homesystem.external.openai;

import lombok.Data;

import java.util.List;

@Data
public class ImageGenerationResponse {

    private List<ResponseData> data;

    @Data
    public static class ResponseData {
        private String url;
        private String b64_json;
    }

}
