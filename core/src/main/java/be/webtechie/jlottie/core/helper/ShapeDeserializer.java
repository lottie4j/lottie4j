package be.webtechie.jlottie.core.helper;

import be.webtechie.jlottie.core.definition.ShapeType;
import be.webtechie.jlottie.core.model.shape.*;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShapeDeserializer extends JsonDeserializer {

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
        ShapeType shapeType = ShapeType.fromValue(node.get("ty").asText());
        switch (shapeType) {
            case ELLIPSE:
                return mapper.convertValue(node, Ellipse.class);
            case FILL:
                return mapper.convertValue(node, Fill.class);
            case GRADIENT_FILL:
                return mapper.convertValue(node, GradientFill.class); // TODO
            case GRADIENT_STROKE:
                return mapper.convertValue(node, GradientStroke.class); // TODO
            case GROUP:
                return mapper.convertValue(node, Group.class);
            case MERGE:
                return mapper.convertValue(node, Merge.class); // TODO
            case NO_STYLE:
                return mapper.convertValue(node, NoStyle.class); // TODO
            case OFFSET_PATH:
                return mapper.convertValue(node, OffsetPath.class); // TODO
            case PATH:
                return mapper.convertValue(node, Path.class); // TODO
            case POLYSTAR:
                return mapper.convertValue(node, Polystar.class); // TODO
            case PUCKER:
                return mapper.convertValue(node, Pucker.class); // TODO
            case RECTANGLE:
                return mapper.convertValue(node, Rectangle.class);
            case REPEATER:
                return mapper.convertValue(node, Repeater.class); // TODO
            case ROUNDED_CORNERS:
                return mapper.convertValue(node, RoundedCorners.class); // TODO
            case STROKE:
                return mapper.convertValue(node, Stroke.class);
            case TRANSFORM:
                return mapper.convertValue(node, Transform.class); // TODO
            case TRIM:
                return mapper.convertValue(node, Trim.class); // TODO
            case TWIST:
                return mapper.convertValue(node, Twist.class); // TODO
            case ZIG_ZAG:
                return mapper.convertValue(node, ZigZag.class); // TODO
            default:
                throw new IllegalArgumentException("Shape could not be parsed");
        }
    }
}
