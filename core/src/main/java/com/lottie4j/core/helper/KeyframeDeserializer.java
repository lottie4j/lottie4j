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
                addKeyframe(rt, childNode);
            }
        } else {
            addKeyframe(rt, node);
        }

        return rt;
    }

    /**
     * Adds what a node is: a keyframe if it is an object, a number of a fixed value if it is a
     * number, and nothing if it is neither.
     *
     * @param keyframes the list to add to
     * @param node      the JSON node to read
     */
    private void addKeyframe(List<Keyframe> keyframes, JsonNode node) {
        if (node.isObject()) {
            // also an object without a time: a damaged keyframe is still a keyframe, not a number
            keyframes.add(mapper.convertValue(node, TimedKeyframe.class));
        } else if (node.isNumber()) {
            keyframes.add(new NumberKeyframe(node.doubleValue()));
        }
    }

    private Double getValue(JsonNode node) {
        if (node instanceof IntNode) {
            return (double) node.intValue();
        }
        return node.doubleValue();
    }
}
