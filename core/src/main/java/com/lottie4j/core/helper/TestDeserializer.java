package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.lottie4j.core.model.test.ShapeA;
import com.lottie4j.core.model.test.ShapeB;
import com.lottie4j.core.model.test.ShapeC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TestDeserializer extends JsonDeserializer {

    ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<Object> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        List<Object> rt = new ArrayList<>();

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node instanceof ArrayNode array) {
            for (Iterator<JsonNode> it = array.elements(); it.hasNext(); ) {
                JsonNode childNode = it.next();
                rt.add(getShape(childNode));
            }
        } else {
            rt.add(getShape(node));
        }

        return rt;
    }

    private Object getShape(JsonNode node) {
        var type = node.get("type").asText();
        switch (type) {
            case "shapeA":
                return mapper.convertValue(node, ShapeA.class);
            case "shapeB":
                return mapper.convertValue(node, ShapeB.class);
            case "shapeC":
                return mapper.convertValue(node, ShapeC.class);
            default:
                throw new IllegalArgumentException("Shape could not be parsed");
        }
    }
}
