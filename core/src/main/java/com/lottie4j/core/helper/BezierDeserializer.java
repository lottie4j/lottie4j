package com.lottie4j.core.helper;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.bezier.Bezier;
import com.lottie4j.core.model.bezier.FixedBezier;
import com.lottie4j.core.model.keyframe.Keyframe;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BezierDeserializer extends JsonDeserializer {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Bezier deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        List<Keyframe> rt = new ArrayList<>();

        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        if (node.get("a").asInt() == 1) {
            return mapper.convertValue(node, AnimatedBezier.class);
        }
        return mapper.convertValue(node, FixedBezier.class);
    }
}
