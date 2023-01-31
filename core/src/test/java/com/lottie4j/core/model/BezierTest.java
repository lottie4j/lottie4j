package com.lottie4j.core.model;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.IOException;

public class BezierTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) {
        String[] jsons = new String[]{
                // jsonLottieFilesFixed
                "{\"a\":0,\"d\":\"test\"}",
                // jsonLottieFilesAnimated
                "{\"a\":1,\"d\":\"test\"}",
                // dotLottieFilesFixed
                "{\"a\":0.0,\"d\":\"test\"}",
                // dotLottieFilesAnimated
                "{\"a\":1.0,\"d\":\"test\"}"
        };
        for (String json : jsons) {
            test(json);
        }
    }

    private static void test(String json) {
        BaseBezier objectFromJson = null;
        try {
            objectFromJson = mapper.readValue(json, BaseBezier.class);
            ObjectMapper mapper = new ObjectMapper();
            String jsonFromObject = mapper.writeValueAsString(objectFromJson);

            System.out.println("Original:\t" + json);
            System.out.println("Generated:\t" + jsonFromObject);

            System.out.println("Is equal: " + json.equals(jsonFromObject));
        } catch (JsonProcessingException e) {
            System.err.println("Json exception: " + e.getMessage());
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "a")
    @JsonSubTypes({
            @JsonSubTypes.Type(names = {"0", "0.0"}, value = FixedBezier.class),
            @JsonSubTypes.Type(names = {"1", "1.0"}, value = AnimatedBezier.class)
    })
    interface BaseBezier {

    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record FixedBezier(
            @JsonProperty("a")
            @JsonSerialize(using = AnimatedSerializer.class)
            Integer animated,

            @JsonProperty("d")
            String data

    ) implements BaseBezier {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AnimatedBezier(
            @JsonProperty("a")
            // @JsonSerialize(using = AnimatedSerializer.class)
            Integer animated,

            @JsonProperty("d")
            String data
    ) implements BaseBezier {
    }

    private static class AnimatedSerializer extends JsonSerializer {
        @Override
        public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeNumber((int) o);
        }
    }
}
