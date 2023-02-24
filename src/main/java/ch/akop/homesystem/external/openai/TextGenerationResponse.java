package ch.akop.homesystem.external.openai;

import lombok.Data;

import java.util.List;

@Data
public class TextGenerationResponse {

    private List<ResponseData> choices;

    @Data
    public static class ResponseData {
        private String text;
    }

}
