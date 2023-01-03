package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class KeyframeSerializer extends JsonSerializer {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * A custom serializer is needed as
     * <ul>
     *     <li>A single value must be written as value</li>
     *     <li>Multiple values must be written as array</li>
     *     <li>Zero value must be written as 0 and not 0.0</li>
     * </ul>
     */
    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        if (o instanceof List<?> list) {
            if (list.size() == 1 && list.get(0) instanceof NumberKeyframe keyframe) {
                jsonGenerator.writeRawValue(keyframe.value().doubleValue() == 0 ? "0" : keyframe.value().toString());
            } else {
                jsonGenerator.writeRawValue("[" + list.stream()
                        .map(v -> getNumberValue((Keyframe) v))
                        .collect(Collectors.joining(",")) + "]");
            }
        }
    }

    private String getNumberValue(Keyframe keyframe) {
        if (keyframe instanceof NumberKeyframe numberKeyframe) {
            return Math.round(numberKeyframe.value().doubleValue()) == numberKeyframe.value().doubleValue() ?
                    String.valueOf(Math.round(numberKeyframe.value().doubleValue())) :
                    String.valueOf(numberKeyframe.value().doubleValue());
        } else if (keyframe instanceof TimedKeyframe timedKeyframe) {
            try {
                return mapper.writeValueAsString(timedKeyframe);
            } catch (JsonProcessingException e) {
                return "";
            }
        }
        return "";
    }
}
