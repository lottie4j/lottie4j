package com.lottie4j.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.handler.FileLoader;
import com.lottie4j.core.model.shape.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BaseShapeTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static Stream<Arguments> provideShapeFiles() {
        return Stream.of(
                Arguments.of("/lottie/shape/ellipse.json", Ellipse.class),
                Arguments.of("/lottie/shape/fill.json", Fill.class),
                Arguments.of("/lottie/shape/gradient_fill.json", GradientFill.class),
                Arguments.of("/lottie/shape/polystar.json", Polystar.class),
                Arguments.of("/lottie/shape/pucker.json", Pucker.class),
                Arguments.of("/lottie/shape/rectangle.json", Rectangle.class),
                Arguments.of("/lottie/shape/repeater_transform.json", Repeater.class),
                Arguments.of("/lottie/shape/rounded_corners.json", RoundedCorners.class),
                Arguments.of("/lottie/shape/stroke_dashes.json", Stroke.class),
                Arguments.of("/lottie/shape/trim_path.json", TrimPath.class)
        );
    }

    @ParameterizedTest
    @MethodSource("provideShapeFiles")
    void shapeFile(String file, Class clazz) throws IOException {
        File f = new File(this.getClass().getResource(file).getFile());
        String jsonFromFile = FileLoader.loadFileAsString(f);
        BaseShape baseShape = mapper.readValue(jsonFromFile, BaseShape.class);

        ObjectMapper mapper = new ObjectMapper();
        String jsonFromObject = mapper.writeValueAsString(baseShape);

        System.out.println("Original:\n" + jsonFromFile.replace("\n", "").replace(" ", ""));
        System.out.println("Generated:\n" + jsonFromObject);

        assertAll(
                () -> assertTrue(clazz.isInstance(baseShape)),
                () -> JSONAssert.assertEquals(jsonFromFile, jsonFromObject, false)
        );
    }
}
