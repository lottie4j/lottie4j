package com.lottie4j.core.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.Path;
import com.lottie4j.core.model.shape.Shape;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class FileLoaderTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static Stream<Arguments> provideLottieFiles() {
        return Stream.of(
                Arguments.of("/lottie/lottie_file/java_duke_empty_layers.json", Animation.class),
                Arguments.of("/lottie/lottie_file/java_duke_layer_1_no_shapes.json", Animation.class),
                Arguments.of("/lottie/lottie_file/java_duke_layer_1_one_shape_only_path.json", Path.class),
                Arguments.of("/lottie/lottie_file/java_duke_layer_1_one_shape_only.json", Shape.class),
                Arguments.of("/lottie/lottie_file/java_duke_layer_1_one_shape.json", Animation.class),
                Arguments.of("/lottie/lottie_file/java_duke_layer_1.json", Animation.class),
                Arguments.of("/lottie/lottie_file/java_duke_layer_2.json", Animation.class),
                Arguments.of("/lottie/lottie_file/java_duke_layer_3.json", Animation.class),
                Arguments.of("/lottie/lottie_file/java_duke_layer_4.json", Animation.class),
                Arguments.of("/lottie/lottie_file/java_duke_layer_5.json", Animation.class),
                Arguments.of("/lottie/lottie_file/java_duke.json", Animation.class),
                //Arguments.of("/lottie/lottie_file/lf20_gOmta2.json", Animation.class),
                //Arguments.of("/lottie/lottie_file/loading.json", Animation.class),
                Arguments.of("/lottie/lottie_file/java_duke_single_layer_no_shapes.json", Layer.class),
                Arguments.of("/lottie/lottie_file/java_duke_single_layer.json", Layer.class),
                Arguments.of("/lottie/lottie_file/java_duke.json", Animation.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideLottieFiles")
    void testFileToObjectToJson(String file, Class clazz) throws IOException {
        File f = new File(this.getClass().getResource(file).getFile());
        String jsonFromFile = FileLoader.loadFileAsString(f);
        var objectFromJson = mapper.readValue(jsonFromFile, clazz);
        ObjectMapper mapper = new ObjectMapper();
        String jsonFromObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectFromJson);

        //System.out.println("Original:\n" + jsonFromFile.replace("\n", "").replace(" ", ""));
        //System.out.println("Generated:\n" + jsonFromObject);

        assertAll(
                () -> assertTrue(clazz.isInstance(objectFromJson)),
                () -> JSONAssert.assertEquals(jsonFromFile, jsonFromObject, false)
        );
    }

    @Test
    void testLoadSingleLayerFileNoShapes() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lottie_file/java_duke_single_layer_no_shapes.json").getFile());
        var jsonFromFile = FileLoader.loadFileAsString(f);
        var l = mapper.readValue(jsonFromFile, Layer.class);
        String jsonFromObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(l);

        assertAll(
                () -> assertNotNull(l),
                () -> assertEquals(3, l.transform().anchor().keyframes().size()),
                () -> JSONAssert.assertEquals(jsonFromFile, jsonFromObject, false)
        );

        System.out.println("Original:\n" + jsonFromFile);
        System.out.println("Generated:\n" + jsonFromObject);
    }

    @Test
    void testLoadSingleLayerFile() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lottie_file/java_duke_single_layer.json").getFile());
        var jsonFromFile = FileLoader.loadFileAsString(f);
        var l = mapper.readValue(jsonFromFile, Layer.class);
        String jsonFromObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(l);

        assertAll(
                () -> assertNotNull(l),
                () -> assertEquals(3, l.transform().anchor().keyframes().size()),
                () -> JSONAssert.assertEquals(jsonFromFile, jsonFromObject, false)
        );

        System.out.println("Original:\n" + jsonFromFile);
        System.out.println("Generated:\n" + jsonFromObject);
    }

    @Test
    void testLoadSmallFile() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lottie_file/java_duke.json").getFile());
        var jsonFromFile = FileLoader.loadFileAsString(f);
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
                () -> JSONAssert.assertEquals(jsonFromFile, jsonFromObject, false)
        );

        System.out.println("Original:\n" + jsonFromFile);
        System.out.println("Generated:\n" + jsonFromObject);
    }

    @Test
    @Disabled("To be completed")
    void testLoadBigFile() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lottie_file/lf20_gOmta2.json").getFile());
        var jsonFromFile = FileLoader.loadFileAsString(f);
        var a = mapper.readValue(jsonFromFile, Animation.class);
        String jsonFromObject = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(a);

        assertAll(
                () -> assertNotNull(a),
                () -> assertEquals("5.5.7", a.version()),
                () -> JSONAssert.assertEquals(jsonFromFile, jsonFromObject, false)
        );

        System.out.println("Original:\n" + jsonFromFile);
        System.out.println("Generated:\n" + jsonFromObject);
    }
}
