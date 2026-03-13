package com.lottie4j.core.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.shape.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LottieJsonFileLoaderTest {

    private static final ObjectMapper mapper = ObjectMapperFactory.getInstance();
    private static final Comparator<JsonNode> JSON_NODE_COMPARATOR = (left, right) -> {
        if (left == null || right == null) {
            return left == right ? 0 : 1;
        }

        if (left.isNumber() && right.isNumber()) {
            return left.decimalValue().compareTo(right.decimalValue());
        }

        return left.equals(right) ? 0 : 1;
    };

    private static Stream<Arguments> provideLottieFiles() {
        return Stream.of(
                Arguments.of("/json/java_duke.json", Animation.class),
                Arguments.of("/json/java_duke_empty_layers.json", Animation.class),
                Arguments.of("/json/java_duke_layer_1_no_shapes.json", Animation.class),
                Arguments.of("/json/java_duke_layer_1_one_shape.json", Animation.class),
                Arguments.of("/json/java_duke_layer_1_one_shape_only.json", BaseShape.class),
                Arguments.of("/json/java_duke_layer_1_one_shape_only_path.json", Path.class),
                Arguments.of("/json/java_duke_layer_1.json", Animation.class),
                Arguments.of("/json/java_duke_layer_2.json", Animation.class),
                Arguments.of("/json/java_duke_layer_3.json", Animation.class),
                Arguments.of("/json/java_duke_layer_4.json", Animation.class),
                Arguments.of("/json/java_duke_layer_5.json", Animation.class),
                Arguments.of("/json/java_duke_single_layer.json", Layer.class),
                Arguments.of("/json/java_duke_single_layer_no_shapes.json", Layer.class),
                Arguments.of("/json/timeline_single_shape_reduced.json", Animation.class),
                Arguments.of("/dot/demo-3-animation.json", Animation.class)
        );
    }

    private static void assertJsonSemanticallyEqual(String expectedJson, String actualJson) throws IOException {
        JsonNode expected = normalizeOptionalFields(mapper.readTree(expectedJson));
        JsonNode actual = normalizeOptionalFields(mapper.readTree(actualJson));

        List<String> differences = findDifferences("", expected, actual);

        assertTrue(differences.isEmpty(), () -> {
            StringBuilder msg = new StringBuilder("JSON mismatch after normalization. Differences found:\n");
            int maxDifferences = Math.min(differences.size(), 20);
            for (int i = 0; i < maxDifferences; i++) {
                msg.append("  ").append(differences.get(i)).append("\n");
            }
            if (differences.size() > maxDifferences) {
                msg.append("  ... and ").append(differences.size() - maxDifferences).append(" more differences\n");
            }
            return msg.toString();
        });
    }

    private static List<String> findDifferences(String path, JsonNode expected, JsonNode actual) {
        List<String> differences = new ArrayList<>();

        if (expected == null && actual == null) {
            return differences;
        }

        if (expected == null) {
            differences.add(path + ": expected null but was " + actual);
            return differences;
        }

        if (actual == null) {
            differences.add(path + ": expected " + expected + " but was null");
            return differences;
        }

        if (expected.getNodeType() != actual.getNodeType()) {
            differences.add(path + ": type mismatch - expected " + expected.getNodeType() + " but was " + actual.getNodeType());
            return differences;
        }

        if (expected.isObject()) {
            // Check all fields in expected
            expected.fieldNames().forEachRemaining(fieldName -> {
                String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
                JsonNode expectedField = expected.get(fieldName);
                JsonNode actualField = actual.get(fieldName);
                differences.addAll(findDifferences(fieldPath, expectedField, actualField));
            });

            // Check for extra fields in actual
            actual.fieldNames().forEachRemaining(fieldName -> {
                if (!expected.has(fieldName)) {
                    String fieldPath = path.isEmpty() ? fieldName : path + "." + fieldName;
                    differences.add(fieldPath + ": unexpected field with value " + actual.get(fieldName));
                }
            });
        } else if (expected.isArray()) {
            if (expected.size() != actual.size()) {
                differences.add(path + ": array size mismatch - expected " + expected.size() + " but was " + actual.size());
            } else {
                for (int i = 0; i < expected.size(); i++) {
                    String indexPath = path + "[" + i + "]";
                    differences.addAll(findDifferences(indexPath, expected.get(i), actual.get(i)));
                }
            }
        } else {
            // Compare values using the comparator
            if (JSON_NODE_COMPARATOR.compare(expected, actual) != 0) {
                differences.add(path + ": expected " + expected + " but was " + actual);
            }
        }

        return differences;
    }

    private static JsonNode normalizeOptionalFields(JsonNode node) {
        if (node == null) {
            return null;
        }

        if (node.isObject()) {
            ObjectNode normalized = node.deepCopy();
            List<String> fieldNames = new ArrayList<>();
            normalized.fieldNames().forEachRemaining(fieldNames::add);
            for (String fieldName : fieldNames) {
                normalized.set(fieldName, normalizeOptionalFields(normalized.get(fieldName)));
            }

            JsonNode markersNode = normalized.get("markers");
            if (markersNode != null && markersNode.isArray() && markersNode.isEmpty()) {
                normalized.remove("markers");
            }

            // Some model classes expose helper/debug views under `list`; ignore them for round-trip JSON checks.
            normalized.remove("list");

            return normalized;
        }

        if (node.isArray()) {
            ArrayNode normalized = mapper.createArrayNode();
            for (JsonNode child : node) {
                normalized.add(normalizeOptionalFields(child));
            }
            return normalized;
        }

        return node;
    }

    @ParameterizedTest
    @MethodSource("provideLottieFiles")
    void testFileToObjectToJson(String file, Class clazz) throws IOException {
        var testFile = this.getClass().getResource(file);

        if (testFile == null) {
            fail("File not found: " + file);
        }

        File f = new File(testFile.getFile());
        String jsonFromFile = Files.readString(f.toPath());
        var objectFromJson = mapper.readValue(jsonFromFile, clazz);
        String jsonFromObject = mapper.writeValueAsString(objectFromJson);

        assertAll(
                () -> assertTrue(clazz.isInstance(objectFromJson)),
                () -> assertJsonSemanticallyEqual(jsonFromFile, jsonFromObject)
        );
    }

    @Test
    void testLoadSingleLayerFileNoShapes() throws IOException {
        File f = new File(this.getClass().getResource("/json/java_duke_single_layer_no_shapes.json").getFile());
        var jsonFromFile = Files.readString(f.toPath());
        var l = mapper.readValue(jsonFromFile, Layer.class);
        String jsonFromObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(l);

        assertAll(
                () -> assertNotNull(l),
                () -> assertEquals(3, l.transform().anchor().keyframes().size()),
                () -> assertJsonSemanticallyEqual(jsonFromFile, jsonFromObject)
        );

        System.out.println("Original:\n" + jsonFromFile);
        System.out.println("Generated:\n" + jsonFromObject);
    }

    @Test
    void testLoadSingleLayerFile() throws IOException {
        File f = new File(this.getClass().getResource("/json/java_duke_single_layer.json").getFile());
        var jsonFromFile = Files.readString(f.toPath());
        var l = mapper.readValue(jsonFromFile, Layer.class);
        String jsonFromObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(l);

        assertAll(
                () -> assertNotNull(l),
                () -> assertEquals(3, l.transform().anchor().keyframes().size()),
                () -> assertJsonSemanticallyEqual(jsonFromFile, jsonFromObject)
        );

        System.out.println("Original:\n" + jsonFromFile);
        System.out.println("Generated:\n" + jsonFromObject);
    }

    @Test
    void testLoadSmallFile() throws IOException {
        File f = new File(this.getClass().getResource("/json/java_duke.json").getFile());
        var jsonFromFile = Files.readString(f.toPath());
        var a = mapper.readValue(jsonFromFile, Animation.class);
        String jsonFromObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(a);

        assertAll(
                () -> assertNotNull(a),
                () -> assertEquals("5.1.20", a.version()),
                () -> assertEquals(0, a.inPoint()),
                () -> assertEquals(38, a.outPoint()),
                () -> assertEquals(550, a.width()),
                () -> assertEquals(400, a.height()),
                () -> assertEquals(5, a.layers().size()),
                () -> assertEquals("Java_Duke_waving", a.layers().get(0).name()),
                () -> assertJsonSemanticallyEqual(jsonFromFile, jsonFromObject)
        );
    }
}
