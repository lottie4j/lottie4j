package com.lottie4j.core.helper;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom serializer for Keyframe lists that handles proper JSON formatting.
 * A custom serializer is needed as:
 * <ul>
 *     <li>A single value must be written as value</li>
 *     <li>Multiple values must be written as array</li>
 *     <li>Zero value must be written as 0 and not 0.0</li>
 * </ul>
 */
public class KeyframeSerializer extends ValueSerializer {

    private final ObjectMapper mapper = ObjectMapperFactory.getInstance();

    /**
     * Default constructor for KeyframeSerializer.
     */
    public KeyframeSerializer() {
        // Default constructor
    }

    /**
     * Serializes a list of keyframes to JSON.
     *
     * @param o the object to serialize (expected to be a List of Keyframes)
     * @param jsonGenerator the JSON generator
     * @param serializerProvider the serializer provider
     * @throws JacksonException if serialization fails
     */
    @Override
    public void serialize(Object o, JsonGenerator jsonGenerator, SerializationContext serializerProvider) throws JacksonException {
        if (o instanceof List<?> list) {
            if (list.size() == 1 && list.get(0) instanceof NumberKeyframe keyframe) {
                jsonGenerator.writeRawValue(keyframe.doubleValue() == 0 ? "0" : keyframe.toString());
            } else {
                jsonGenerator.writeRawValue("[" + list.stream()
                        .map(v -> getNumberValue((Keyframe) v))
                        .collect(Collectors.joining(",")) + "]");
            }
        }
    }

    /**
     * Converts a keyframe to its JSON string representation.
     * Formats numbers as integers when appropriate and handles timed keyframes.
     *
     * @param keyframe the keyframe to convert
     * @return the string representation
     */
    private String getNumberValue(Keyframe keyframe) {
        if (keyframe instanceof NumberKeyframe numberKeyframe) {
            return Math.round(numberKeyframe.doubleValue()) == numberKeyframe.doubleValue() ?
                    String.valueOf(Math.round(numberKeyframe.doubleValue())) :
                    String.valueOf(numberKeyframe.doubleValue());
        } else if (keyframe instanceof TimedKeyframe timedKeyframe) {
            try {
                return mapper.writeValueAsString(timedKeyframe);
            } catch (JacksonException e) {
                return "";
            }
        }
        return "";
    }
}
