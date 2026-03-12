package com.lottie4j.core.definition;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DefinitionTest {

    @Test
    void shapeTypesTest() {
        ObjectMapper mapper = new ObjectMapper();

        assertAll(
                () -> assertEquals(ShapeType.ELLIPSE, mapper.readValue("{\"u\": \"el\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.FILL, mapper.readValue("{\"u\": \"fl\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.GRADIENT_FILL, mapper.readValue("{\"u\": \"gf\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.GRADIENT_STROKE, mapper.readValue("{\"u\": \"gs\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.GROUP, mapper.readValue("{\"u\": \"gr\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.MERGE, mapper.readValue("{\"u\": \"mm\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.NO_STYLE, mapper.readValue("{\"u\": \"no\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.OFFSET_PATH, mapper.readValue("{\"u\": \"op\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.PATH, mapper.readValue("{\"u\": \"sh\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.POLYSTAR, mapper.readValue("{\"u\": \"sr\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.PUCKER, mapper.readValue("{\"u\": \"pb\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.RECTANGLE, mapper.readValue("{\"u\": \"rc\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.REPEATER, mapper.readValue("{\"u\": \"rp\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.ROUNDED_CORNERS, mapper.readValue("{\"u\": \"rd\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.STROKE, mapper.readValue("{\"u\": \"st\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.TRANSFORM, mapper.readValue("{\"u\": \"tr\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.TRIM, mapper.readValue("{\"u\": \"tm\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.TWIST, mapper.readValue("{\"u\": \"tw\"}", CheckRecord.class).shapeType),
                () -> assertEquals(ShapeType.ZIG_ZAG, mapper.readValue("{\"u\": \"zz\"}", CheckRecord.class).shapeType)
        );
    }

    @Test
    void definitionsTest() {
        ObjectMapper mapper = new ObjectMapper();

        assertAll(
                () -> assertEquals(BlendMode.NORMAL, mapper.readValue("{\"bm\": 0}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.MULTIPLY, mapper.readValue("{\"bm\": 1}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.SCREEN, mapper.readValue("{\"bm\": 2}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.OVERLAY, mapper.readValue("{\"bm\": 3}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.DARKEN, mapper.readValue("{\"bm\": 4}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.LIGHTEN, mapper.readValue("{\"bm\": 5}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.COLOR_DODGE, mapper.readValue("{\"bm\": 6}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.COLOR_BURN, mapper.readValue("{\"bm\": 7}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.HARD_LIGHT, mapper.readValue("{\"bm\": 8}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.SOFT_LIGHT, mapper.readValue("{\"bm\": 9}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.DIFFERENCE, mapper.readValue("{\"bm\": 10}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.EXCLUSION, mapper.readValue("{\"bm\": 11}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.HUE, mapper.readValue("{\"bm\": 12}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.SATURATION, mapper.readValue("{\"bm\": 13}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.COLOR, mapper.readValue("{\"bm\": 14}", CheckRecord.class).blendMode),
                () -> assertEquals(BlendMode.LUMINOSITY, mapper.readValue("{\"bm\": 15}", CheckRecord.class).blendMode),

                () -> assertEquals(Composite.ABOVE, mapper.readValue("{\"m-c\": 1}", CheckRecord.class).stackingOrder),
                () -> assertEquals(Composite.BELOW, mapper.readValue("{\"m-c\": 2}", CheckRecord.class).stackingOrder),

                () -> assertEquals(EffectType.NORMAL, mapper.readValue("{\"ty-e\": 5}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.PAINT_OVER_TRANSPARENT, mapper.readValue("{\"ty-e\": 7}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.TINT, mapper.readValue("{\"ty-e\": 20}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.FILL, mapper.readValue("{\"ty-e\": 21}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.STROKE, mapper.readValue("{\"ty-e\": 22}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.TRITONE, mapper.readValue("{\"ty-e\": 23}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.PRO_LEVELS, mapper.readValue("{\"ty-e\": 24}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.DROP_SHADOW, mapper.readValue("{\"ty-e\": 25}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.RADIAL_WIPE, mapper.readValue("{\"ty-e\": 26}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.DISPLACEMENT_MAP, mapper.readValue("{\"ty-e\": 27}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.MATTE3, mapper.readValue("{\"ty-e\": 28}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.GAUSSIAN_BLUR, mapper.readValue("{\"ty-e\": 29}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.MESH_WARP, mapper.readValue("{\"ty-e\": 31}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.WAVY, mapper.readValue("{\"ty-e\": 32}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.SPHERIZE, mapper.readValue("{\"ty-e\": 33}", CheckRecord.class).effectType),
                () -> assertEquals(EffectType.PUPPET, mapper.readValue("{\"ty-e\": 34}", CheckRecord.class).effectType),

                () -> assertEquals(FillRule.NON_ZERO, mapper.readValue("{\"r\": 1}", CheckRecord.class).fillRule),
                () -> assertEquals(FillRule.EVEN_ODD, mapper.readValue("{\"r\": 2}", CheckRecord.class).fillRule),

                () -> assertEquals(GradientType.LINEAR, mapper.readValue("{\"t\": 1}", CheckRecord.class).gradientType),
                () -> assertEquals(GradientType.RADIAL, mapper.readValue("{\"t\": 2}", CheckRecord.class).gradientType),

                () -> assertEquals(LayerType.PRECOMPOSITION, mapper.readValue("{\"ty-l\": 0}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.SOLD_COLOR, mapper.readValue("{\"ty-l\": 1}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.IMAGE, mapper.readValue("{\"ty-l\": 2}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.NULL, mapper.readValue("{\"ty-l\": 3}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.SHAPE, mapper.readValue("{\"ty-l\": 4}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.TEXT, mapper.readValue("{\"ty-l\": 5}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.AUDIO, mapper.readValue("{\"ty-l\": 6}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.VIDEO_PLACEHOLDER, mapper.readValue("{\"ty-l\": 7}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.IMAGE_SEQUENCE, mapper.readValue("{\"ty-l\": 8}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.VIDEO, mapper.readValue("{\"ty-l\": 9}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.IMAGE_PLACEHOLDER, mapper.readValue("{\"ty-l\": 10}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.GUIDE, mapper.readValue("{\"ty-l\": 11}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.ADJUSTMENT, mapper.readValue("{\"ty-l\": 12}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.CAMERA, mapper.readValue("{\"ty-l\": 13}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.LIGHT, mapper.readValue("{\"ty-l\": 14}", CheckRecord.class).layerType),
                () -> assertEquals(LayerType.DATA, mapper.readValue("{\"ty-l\": 15}", CheckRecord.class).layerType),

                () -> assertEquals(LineCap.BUTT, mapper.readValue("{\"lc\": 1}", CheckRecord.class).lineCap),
                () -> assertEquals(LineCap.ROUND, mapper.readValue("{\"lc\": 2}", CheckRecord.class).lineCap),
                () -> assertEquals(LineCap.SQUARE, mapper.readValue("{\"lc\": 3}", CheckRecord.class).lineCap),

                () -> assertEquals(LineJoin.MITER, mapper.readValue("{\"lj\": 1}", CheckRecord.class).lineJoin),
                () -> assertEquals(LineJoin.ROUND, mapper.readValue("{\"lj\": 2}", CheckRecord.class).lineJoin),
                () -> assertEquals(LineJoin.BEVEL, mapper.readValue("{\"lj\": 3}", CheckRecord.class).lineJoin),

                () -> assertEquals(MatteMode.NORMAL, mapper.readValue("{\"tt\": 0}", CheckRecord.class).matteMode),
                () -> assertEquals(MatteMode.ALPHA, mapper.readValue("{\"tt\": 1}", CheckRecord.class).matteMode),
                () -> assertEquals(MatteMode.INVERTED_ALPHA, mapper.readValue("{\"tt\": 2}", CheckRecord.class).matteMode),
                () -> assertEquals(MatteMode.LUMA, mapper.readValue("{\"tt\": 3}", CheckRecord.class).matteMode),
                () -> assertEquals(MatteMode.INVERTED_LUMA, mapper.readValue("{\"tt\": 4}", CheckRecord.class).matteMode),

                () -> assertEquals(MergeMode.NORMAL, mapper.readValue("{\"mm\": 1}", CheckRecord.class).mergeMode),
                () -> assertEquals(MergeMode.ADD, mapper.readValue("{\"mm\": 2}", CheckRecord.class).mergeMode),
                () -> assertEquals(MergeMode.SUBTRACT, mapper.readValue("{\"mm\": 3}", CheckRecord.class).mergeMode),
                () -> assertEquals(MergeMode.INTERSECT, mapper.readValue("{\"mm\": 4}", CheckRecord.class).mergeMode),
                () -> assertEquals(MergeMode.EXCLUDE, mapper.readValue("{\"mm\": 5}", CheckRecord.class).mergeMode),

                () -> assertEquals(StarType.STAR, mapper.readValue("{\"sy\": 1}", CheckRecord.class).starType),
                () -> assertEquals(StarType.POLYGON, mapper.readValue("{\"sy\": 2}", CheckRecord.class).starType),

                () -> assertEquals(StrokeDashType.DASH, mapper.readValue("{\"n\": \"d\"}", CheckRecord.class).strokeDashType),
                () -> assertEquals(StrokeDashType.GAP, mapper.readValue("{\"n\": \"g\"}", CheckRecord.class).strokeDashType),
                () -> assertEquals(StrokeDashType.OFFSET, mapper.readValue("{\"n\": \"o\"}", CheckRecord.class).strokeDashType),

                () -> assertEquals(TrimMultipleShapes.INDIVIDUALLY, mapper.readValue("{\"m-t\": 1}", CheckRecord.class).trimMultipleShapes),
                () -> assertEquals(TrimMultipleShapes.SIMULTANEOUSLY, mapper.readValue("{\"m-t\": 2}", CheckRecord.class).trimMultipleShapes)
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record CheckRecord(
            @JsonProperty("u") ShapeType shapeType,
            @JsonProperty("bm") BlendMode blendMode,
            @JsonProperty("m-c") Composite stackingOrder,
            @JsonProperty("ty-e") EffectType effectType,
            @JsonProperty("r") FillRule fillRule,
            @JsonProperty("t") GradientType gradientType,
            @JsonProperty("ty-l") LayerType layerType,
            @JsonProperty("lc") LineCap lineCap,
            @JsonProperty("lj") LineJoin lineJoin,
            @JsonProperty("tt") MatteMode matteMode,
            @JsonProperty("mm") MergeMode mergeMode,

            @JsonProperty("sy") StarType starType,
            @JsonProperty("n") StrokeDashType strokeDashType,
            @JsonProperty("m-t") TrimMultipleShapes trimMultipleShapes
    ) {
    }
}
