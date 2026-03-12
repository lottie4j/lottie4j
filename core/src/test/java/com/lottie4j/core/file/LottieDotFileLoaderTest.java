package com.lottie4j.core.file;

import com.lottie4j.core.definition.ShapeGroup;
import com.lottie4j.core.definition.ShapeType;
import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Asset;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.dot.DotLottie;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class LottieDotFileLoaderTest {

    private static Stream<Arguments> provideLottieFiles() {
        return Stream.of(
                Arguments.of("/dot/demo-1.lottie", 1),
                Arguments.of("/dot/demo-2.lottie", 1),
                Arguments.of("/dot/demo-3.lottie", 1),
                Arguments.of("/dot/lottie4j.lottie", 1)
        );
    }

    @ParameterizedTest
    @MethodSource("provideLottieFiles")
    void testLoadDotLottieShouldContainAnimation(String file, int numberOfAnimations) throws LottieFileException {
        var testFile = this.getClass().getResource(file);

        if (testFile == null) {
            fail("File not found: " + file);
        }

        File f = new File(testFile.getFile());
        DotLottie dotLottie = LottieFileLoader.loadDotLottie(f);

        assertAll(
                () -> assertNotNull(dotLottie),
                () -> assertEquals(numberOfAnimations, dotLottie.animations().size())
        );
    }

    @ParameterizedTest
    @MethodSource("provideLottieFiles")
    void testLoadShouldReturnAnimation(String file) throws LottieFileException {
        var testFile = this.getClass().getResource(file);

        if (testFile == null) {
            fail("File not found: " + file);
        }

        File f = new File(testFile.getFile());
        Animation animation = LottieFileLoader.load(f);
        assertNotNull(animation);
    }

    @Test
    void testDemo3ContainsModifierShapes() throws LottieFileException {
        var testFile = this.getClass().getResource("/dot/demo-3.lottie");
        if (testFile == null) {
            fail("File not found: /dot/demo-3.lottie");
        }

        Animation animation = LottieFileLoader.load(new File(testFile.getFile()));
        assertNotNull(animation);
        assertNotNull(animation.layers());

        Set<ShapeType> modifierTypes = new HashSet<>();
        collectModifierTypesFromLayers(animation.layers(), modifierTypes);

        if (animation.assets() != null) {
            for (Asset asset : animation.assets()) {
                collectModifierTypesFromLayers(asset.layers(), modifierTypes);
            }
        }

        assertFalse(modifierTypes.isEmpty(), "Expected at least one modifier shape in demo-3.lottie");
        assertTrue(modifierTypes.contains(ShapeType.MERGE), "Expected merge modifier support fixture in demo-3.lottie");
    }

    private void collectModifierTypesFromLayers(java.util.List<Layer> layers, Set<ShapeType> modifierTypes) {
        if (layers == null) {
            return;
        }
        for (Layer layer : layers) {
            collectModifierTypes(layer.shapes(), modifierTypes);
        }
    }

    private void collectModifierTypes(java.util.List<BaseShape> shapes, Set<ShapeType> modifierTypes) {
        if (shapes == null) {
            return;
        }

        for (BaseShape shape : shapes) {
            if (shape == null || shape.shapeType() == null) {
                continue;
            }
            if (shape.shapeType().shapeGroup() == ShapeGroup.MODIFIER) {
                modifierTypes.add(shape.shapeType());
            }
            if (shape instanceof Group group) {
                collectModifierTypes(group.shapes(), modifierTypes);
            }
        }
    }
}
