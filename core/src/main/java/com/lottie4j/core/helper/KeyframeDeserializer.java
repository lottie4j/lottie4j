package com.lottie4j.core.helper;

import tools.jackson.core.JacksonException;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom deserializer for Keyframe objects that handles both single values and arrays.
 * Determines whether to create TimedKeyframe or NumberKeyframe based on JSON structure.
 */
public class KeyframeDeserializer extends ValueDeserializer {

    private static final ObjectMapper mapper = ObjectMapperFactory.getInstance();

    /**
     * Default constructor for KeyframeDeserializer.
     */
    public KeyframeDeserializer() {
        // Default constructor
    }

    /**
     * Deserializes JSON into a list of Keyframe objects.
     * Handles both array and single value representations.
     *
     * @param jsonParser the JSON parser
     * @param deserializationContext the deserialization context
     * @return list of deserialized keyframes
     * @throws JacksonException if parsing fails
     */
    @Override
    public List<Keyframe> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        List<Keyframe> rt = new ArrayList<>();

        JsonNode node = (jsonParser.readValueAsTree());

        if (node instanceof ArrayNode array) {
            for (JsonNode childNode : array.elements()) {
                rt.add(getKeyframe(childNode));
            }
        } else {
            rt.add(getKeyframe(node));
        }

        return rt;
    }

    /**
     * Determines the appropriate Keyframe type based on JSON node structure.
     *
     * @param node the JSON node to parse
     * @return a TimedKeyframe if timing fields are present, otherwise a NumberKeyframe
     */
    private Keyframe getKeyframe(JsonNode node) {
        if (node.has("i") || node.has("o") || node.has("t") || node.has("x")) {
            return mapper.convertValue(node, TimedKeyframe.class);
        } else {
            return new NumberKeyframe(node.doubleValue());
        }
    }

    /**
     * Extracts a double value from a JSON node, handling both integer and decimal types.
     *
     * @param node the JSON node
     * @return the double value
     */
    private Double getValue(JsonNode node) {
        if (node instanceof IntNode) {
            return (double) node.intValue();
        }
        return node.doubleValue();
    }
}
