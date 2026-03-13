package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Custom deserializer for Double lists that handles both single values and arrays.
 * Normalizes single double values into a list for consistent handling.
 */
public class ListDoubleDeserializer extends JsonDeserializer {

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
    public List<Double> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        List<Double> rt = new ArrayList<>();

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node instanceof ArrayNode array) {
            for (Iterator<JsonNode> it = array.elements(); it.hasNext(); ) {
                JsonNode childNode = it.next();
                rt.add(childNode.doubleValue());
            }
        } else {
            rt.add(node.doubleValue());
        }

        return rt;
    }
}
