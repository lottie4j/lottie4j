package com.lottie4j.core.helper;

import tools.jackson.core.JacksonException;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom deserializer for Double lists that handles both single values and arrays.
 * Normalizes single double values into a list for consistent handling.
 */
public class ListDoubleDeserializer extends ValueDeserializer {

    /**
     * Default constructor for ListDoubleDeserializer.
     */
    public ListDoubleDeserializer() {
        // Default constructor
    }

    /**
     * Deserializes JSON into a list of Double values.
     * Accepts both array notation and single values.
     *
     * @param jsonParser the JSON parser
     * @param deserializationContext the deserialization context
     * @return list of double values
     * @throws IOException if parsing fails
     */
    @Override
    public List<Double> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        List<Double> rt = new ArrayList<>();

        JsonNode node = (jsonParser.readValueAsTree());

        if (node instanceof ArrayNode array) {
            for (JsonNode childNode : array.elements()) {
                rt.add(childNode.doubleValue());
            }
        } else {
            rt.add(node.doubleValue());
        }

        return rt;
    }
}
