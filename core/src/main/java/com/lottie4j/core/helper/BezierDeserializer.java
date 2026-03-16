package com.lottie4j.core.helper;

import tools.jackson.core.JacksonException;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.Bezier;
import com.lottie4j.core.model.bezier.FixedBezier;


/**
 * Because animated and non-animated beziers have a different JSON-structure, it was not able to just use Jackson to parse these.
 * This Deserializer helps to create the right type of object.
 */
public class BezierDeserializer extends ValueDeserializer {

    private static final ObjectMapper mapper = ObjectMapperFactory.getInstance();

    /**
     * Default constructor for BezierDeserializer.
     */
    public BezierDeserializer() {
        // Default constructor
    }

    /**
     * Deserializes a JSON node into the appropriate Bezier type.
     * Determines whether to create an AnimatedBezier or FixedBezier based on the "a" field.
     *
     * @param jsonParser             the JSON parser
     * @param deserializationContext the deserialization context
     * @return an AnimatedBezier if animated flag is set, otherwise a FixedBezier
     * @throws JacksonException if parsing fails
     */
    @Override
    public Bezier deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        JsonNode node = (jsonParser.readValueAsTree());
        if (node.get("a") != null && node.get("a").asInt() == 1) {
            return mapper.convertValue(node, AnimatedBezier.class);
        }
        return mapper.convertValue(node, FixedBezier.class);
    }
}
