package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.Bezier;
import com.lottie4j.core.model.bezier.FixedBezier;

import java.io.IOException;

/**
 * Because animated and non-animated beziers have a different JSON-structure, it was not able to just use Jackson to parse these.
 * This Deserializer helps to create the right type of object.
 */
public class BezierDeserializer extends JsonDeserializer {

    private static final ObjectMapper mapper = ObjectMapperFactory.getInstance();

    /**
     * Deserializes a JSON node into the appropriate Bezier type.
     * Determines whether to create an AnimatedBezier or FixedBezier based on the "a" field.
     *
     * @param jsonParser             the JSON parser
     * @param deserializationContext the deserialization context
     * @return an AnimatedBezier if animated flag is set, otherwise a FixedBezier
     * @throws IOException if parsing fails
     */
    @Override
    public Bezier deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        if (node.get("a") != null && node.get("a").asInt() == 1) {
            return mapper.convertValue(node, AnimatedBezier.class);
        }
        return mapper.convertValue(node, FixedBezier.class);
    }
}
