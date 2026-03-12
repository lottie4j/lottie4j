package com.lottie4j.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.modifier.Pucker;
import com.lottie4j.core.model.shape.modifier.Repeater;
import com.lottie4j.core.model.shape.modifier.RoundedCorners;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.core.model.shape.shape.Ellipse;
import com.lottie4j.core.model.shape.shape.Polystar;
import com.lottie4j.core.model.shape.shape.Rectangle;
import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.core.model.shape.style.GradientFill;
import com.lottie4j.core.model.shape.style.Stroke;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class BaseShapeTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseShapeTest.class);

    private static final ObjectMapper mapper = ObjectMapperFactory.getInstance();

    private static Stream<Arguments> provideShapeFiles() {
        return Stream.of(
                Arguments.of("/shape/ellipse.json", Ellipse.class, ShapeType.ELLIPSE),
                Arguments.of("/shape/fill.json", Fill.class, ShapeType.FILL),
                Arguments.of("/shape/gradient_fill.json", GradientFill.class, ShapeType.GRADIENT_FILL),
                Arguments.of("/shape/polystar.json", Polystar.class, ShapeType.POLYSTAR),
                Arguments.of("/shape/pucker.json", Pucker.class, ShapeType.PUCKER),
                Arguments.of("/shape/rectangle.json", Rectangle.class, ShapeType.RECTANGLE),
                Arguments.of("/shape/repeater.json", Repeater.class, ShapeType.REPEATER),
                Arguments.of("/shape/rounded_corners.json", RoundedCorners.class, ShapeType.ROUNDED_CORNERS),
                Arguments.of("/shape/stroke_dashes.json", Stroke.class, ShapeType.STROKE),
                Arguments.of("/shape/trim_path.json", TrimPath.class, ShapeType.TRIM)
        );
    }

    @ParameterizedTest
    @MethodSource("provideShapeFiles")
    void shapeFile(String file, Class clazz, ShapeType type) throws IOException {
        File f = new File(this.getClass().getResource(file).getFile());
        String jsonFromFile = Files.readString(f.toPath());
        BaseShape baseShape = mapper.readValue(jsonFromFile, BaseShape.class);

        String jsonFromObject = mapper.writeValueAsString(baseShape);

        logger.info("Original:\n{}", jsonFromFile.replace("\n", "").replace(" ", ""));
        logger.info("Generated:\n{}", jsonFromObject);

        assertAll(
                () -> assertTrue(clazz.isInstance(baseShape)),
                () -> assertEquals(type, baseShape.shapeType()),
                () -> JSONAssert.assertEquals(jsonFromFile, jsonFromObject, false)
        );
    }
}
