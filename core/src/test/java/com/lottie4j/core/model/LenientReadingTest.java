package com.lottie4j.core.model;

import com.lottie4j.core.definition.BlendMode;
import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.core.model.text.Font;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real Lottie files carry things this library does not render or does not know. They must still
 * load, with everything else intact, and keep what they carried when they are written again.
 */
class LenientReadingTest {

    private static final String KS = "\"ks\":{\"o\":{\"a\":0,\"k\":100},\"p\":{\"a\":0,\"k\":[0,0,0]}}";

    private static String document(String top, String layer) {
        return "{\"v\":\"5.7.4\",\"fr\":30,\"ip\":0,\"op\":60,\"w\":100,\"h\":100," + top + "\"layers\":[" + layer + "]}";
    }

    private static Animation read(String json) {
        return ObjectMapperFactory.getInstance().readValue(json, Animation.class);
    }

    private static <T> T read(String json, Class<T> type) {
        return ObjectMapperFactory.getInstance().readValue(json, type);
    }

    private static String write(Object value) {
        return ObjectMapperFactory.getInstance().writeValueAsString(value);
    }

    @Test
    void aPropertyWithAnExpressionKeepsItsValueAndTheExpression() throws Exception {
        String json = "{\"a\":0,\"k\":100,\"x\":\"var $bm_rt = 50;\"}";

        Animated property = read(json, Animated.class);

        assertTrue(property.hasExpression());
        assertEquals("var $bm_rt = 50;", property.expression());
        assertNull(property.x());
        assertEquals(100.0, property.getValue(0));
        JSONAssert.assertEquals(json, write(property), true);
    }

    @Test
    void aSplitPositionStillHasItsDimensions() throws Exception {
        String json = "{\"s\":true,\"x\":{\"a\":0,\"k\":5},\"y\":{\"a\":0,\"k\":6}}";

        Animated position = read(json, Animated.class);

        assertFalse(position.hasExpression());
        assertEquals(5.0, position.x().getValue(0));
        assertEquals(6.0, position.y().getValue(0));
        JSONAssert.assertEquals(json, write(position), true);
    }

    @Test
    void aDimensionOfASplitPositionMayHaveAnExpression() {
        Animated position = read(
                "{\"s\":true,\"x\":{\"a\":0,\"k\":5,\"x\":\"wiggle(1, 2)\"},\"y\":{\"a\":0,\"k\":6}}", Animated.class);

        assertFalse(position.hasExpression());
        assertTrue(position.x().hasExpression());
        assertEquals(5.0, position.x().getValue(0));
    }

    @Test
    void aPropertyWrittenAsABareNumberIsAFixedValue() {
        Layer layer = read(document("", "{\"ty\":4,\"ks\":{\"o\":100}}")).layers().get(0);

        assertEquals(100.0, layer.transform().opacity().getValue(0));
    }

    @Test
    void aFractionalFrameRateIsKept() throws Exception {
        String json = document("", "{\"ty\":4," + KS + "}").replace("\"fr\":30", "\"fr\":29.97")
                .replace("\"op\":60", "\"op\":60.5");

        Animation animation = read(json);

        assertEquals(29.97, animation.framesPerSecond());
        assertEquals(60.5, animation.outPoint());
        assertTrue(write(animation).contains("\"fr\":29.97"));
    }

    @Test
    void wholeTimingValuesAreWrittenWithoutDecimals() {
        String written = write(read(document("", "{\"ty\":4," + KS + "}")));

        assertTrue(written.contains("\"fr\":30,"), written);
        assertTrue(written.contains("\"op\":60,"), written);
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"ty\":99", "\"ty\":4,\"bm\":99", "\"ty\":4,\"tt\":9", "\"ty\":\"shape\""})
    void anUnknownDefinitionValueDoesNotRejectTheFile(String members) {
        Animation animation = read(document("", "{" + members + ",\"nm\":\"odd\"," + KS + "}"));

        Layer layer = animation.layers().get(0);
        assertEquals("odd", layer.name());
        assertNotNull(layer.transform());
    }

    @Test
    void anUnknownValueReadsAsAbsent() {
        Layer layer = read(document("", "{\"ty\":99,\"bm\":99," + KS + "}")).layers().get(0);

        assertNull(layer.layerType());
        assertNull(layer.blendMode());
    }

    @Test
    void knownValuesStillRead() {
        Layer layer = read(document("", "{\"ty\":4.0,\"bm\":1," + KS + "}")).layers().get(0);

        assertEquals(LayerType.SHAPE, layer.layerType());
        assertEquals(BlendMode.MULTIPLY, layer.blendMode());
    }

    @Test
    void fontsAreReadAndFoundByName() throws Exception {
        String fonts = "\"fonts\":{\"list\":[{\"fName\":\"Arial-BoldMT\",\"fFamily\":\"Arial\",\"fStyle\":\"Bold\","
                + "\"ascent\":71.6}]},";

        Animation animation = read(document(fonts, "{\"ty\":4," + KS + "}"));

        Font font = animation.fonts().byName("Arial-BoldMT");
        assertEquals("Arial", font.family());
        assertEquals("Bold", font.style());
        assertEquals(71.6, font.ascent());
        assertNull(animation.fonts().byName("Other"));
        assertTrue(write(animation).contains("\"fFamily\":\"Arial\""));
    }

    @Test
    void aSolidLayerHasItsSizeAndALayerMayHaveStyles() {
        Layer layer = read(document("", "{\"ty\":1,\"sc\":\"#ff0000\",\"sw\":40,\"sh\":20,\"sy\":[{\"ty\":1}]," + KS
                + "}")).layers().get(0);

        assertEquals(40.0, layer.solidWidth());
        assertEquals(20.0, layer.solidHeight());
        assertEquals(1, layer.styles().size());
        assertTrue(write(layer).contains("\"sw\":40,"));
    }

    @Test
    void aShapeOfAnUnknownTypeStandsInForItselfAndTheOthersAreRead() {
        Layer layer = read(document("", "{\"ty\":4," + KS + ",\"shapes\":[{\"ty\":\"xx\",\"nm\":\"new\"},"
                + "{\"ty\":\"el\",\"nm\":\"dot\",\"p\":{\"a\":0,\"k\":[0,0]},\"s\":{\"a\":0,\"k\":[4,4]}}]}"))
                .layers().get(0);

        assertEquals(2, layer.shapes().size());
        assertEquals(com.lottie4j.core.definition.ShapeType.UNKNOWN, layer.shapes().get(0).shapeType());
        assertEquals("new", layer.shapes().get(0).name());
        assertEquals(com.lottie4j.core.definition.ShapeType.ELLIPSE, layer.shapes().get(1).shapeType());
    }
}
