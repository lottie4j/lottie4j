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
                () -> assertEquals(mapper.readValue("{\"u\": \"el\"}", CheckRecord.class).shapeType, ShapeType.ELLIPSE),
                () -> assertEquals(mapper.readValue("{\"u\": \"fl\"}", CheckRecord.class).shapeType, ShapeType.FILL),
                () -> assertEquals(mapper.readValue("{\"u\": \"gf\"}", CheckRecord.class).shapeType, ShapeType.GRADIENT_FILL),
                () -> assertEquals(mapper.readValue("{\"u\": \"gs\"}", CheckRecord.class).shapeType, ShapeType.GRADIENT_STROKE),
                () -> assertEquals(mapper.readValue("{\"u\": \"gr\"}", CheckRecord.class).shapeType, ShapeType.GROUP),
                () -> assertEquals(mapper.readValue("{\"u\": \"mm\"}", CheckRecord.class).shapeType, ShapeType.MERGE),
                () -> assertEquals(mapper.readValue("{\"u\": \"no\"}", CheckRecord.class).shapeType, ShapeType.NO_STYLE),
                () -> assertEquals(mapper.readValue("{\"u\": \"op\"}", CheckRecord.class).shapeType, ShapeType.OFFSET_PATH),
                () -> assertEquals(mapper.readValue("{\"u\": \"sh\"}", CheckRecord.class).shapeType, ShapeType.PATH),
                () -> assertEquals(mapper.readValue("{\"u\": \"sr\"}", CheckRecord.class).shapeType, ShapeType.POLYSTAR),
                () -> assertEquals(mapper.readValue("{\"u\": \"pb\"}", CheckRecord.class).shapeType, ShapeType.PUCKER),
                () -> assertEquals(mapper.readValue("{\"u\": \"rc\"}", CheckRecord.class).shapeType, ShapeType.RECTANGLE),
                () -> assertEquals(mapper.readValue("{\"u\": \"rp\"}", CheckRecord.class).shapeType, ShapeType.REPEATER),
                () -> assertEquals(mapper.readValue("{\"u\": \"rd\"}", CheckRecord.class).shapeType, ShapeType.ROUNDED_CORNERS),
                () -> assertEquals(mapper.readValue("{\"u\": \"st\"}", CheckRecord.class).shapeType, ShapeType.STROKE),
                () -> assertEquals(mapper.readValue("{\"u\": \"tr\"}", CheckRecord.class).shapeType, ShapeType.TRANSFORM),
                () -> assertEquals(mapper.readValue("{\"u\": \"tm\"}", CheckRecord.class).shapeType, ShapeType.TRIM),
                () -> assertEquals(mapper.readValue("{\"u\": \"tw\"}", CheckRecord.class).shapeType, ShapeType.TWIST),
                () -> assertEquals(mapper.readValue("{\"u\": \"zz\"}", CheckRecord.class).shapeType, ShapeType.ZIG_ZAG)
        );
    }

    @Test
    void definitionsTest() {
        ObjectMapper mapper = new ObjectMapper();

        assertAll(
                () -> assertEquals(mapper.readValue("{\"bm\": 0}", CheckRecord.class).blendMode, BlendMode.NORMAL),
                () -> assertEquals(mapper.readValue("{\"bm\": 1}", CheckRecord.class).blendMode, BlendMode.MULTIPLY),
                () -> assertEquals(mapper.readValue("{\"bm\": 2}", CheckRecord.class).blendMode, BlendMode.SCREEN),
                () -> assertEquals(mapper.readValue("{\"bm\": 3}", CheckRecord.class).blendMode, BlendMode.OVERLAY),
                () -> assertEquals(mapper.readValue("{\"bm\": 4}", CheckRecord.class).blendMode, BlendMode.DARKEN),
                () -> assertEquals(mapper.readValue("{\"bm\": 5}", CheckRecord.class).blendMode, BlendMode.LIGHTEN),
                () -> assertEquals(mapper.readValue("{\"bm\": 6}", CheckRecord.class).blendMode, BlendMode.COLOR_DODGE),
                () -> assertEquals(mapper.readValue("{\"bm\": 7}", CheckRecord.class).blendMode, BlendMode.COLOR_BURN),
                () -> assertEquals(mapper.readValue("{\"bm\": 8}", CheckRecord.class).blendMode, BlendMode.HARD_LIGHT),
                () -> assertEquals(mapper.readValue("{\"bm\": 9}", CheckRecord.class).blendMode, BlendMode.SOFT_LIGHT),
                () -> assertEquals(mapper.readValue("{\"bm\": 10}", CheckRecord.class).blendMode, BlendMode.DIFFERENCE),
                () -> assertEquals(mapper.readValue("{\"bm\": 11}", CheckRecord.class).blendMode, BlendMode.EXCLUSION),
                () -> assertEquals(mapper.readValue("{\"bm\": 12}", CheckRecord.class).blendMode, BlendMode.HUE),
                () -> assertEquals(mapper.readValue("{\"bm\": 13}", CheckRecord.class).blendMode, BlendMode.SATURATION),
                () -> assertEquals(mapper.readValue("{\"bm\": 14}", CheckRecord.class).blendMode, BlendMode.COLOR),
                () -> assertEquals(mapper.readValue("{\"bm\": 15}", CheckRecord.class).blendMode, BlendMode.LUMINOSITY),

                () -> assertEquals(mapper.readValue("{\"m-c\": 1}", CheckRecord.class).stackingOrder, Composite.ABOVE),
                () -> assertEquals(mapper.readValue("{\"m-c\": 2}", CheckRecord.class).stackingOrder, Composite.BELOW),

                () -> assertEquals(mapper.readValue("{\"ty-e\": 5}", CheckRecord.class).effectType, EffectType.NORMAL),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 7}", CheckRecord.class).effectType, EffectType.PAINT_OVER_TRANSPARENT),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 20}", CheckRecord.class).effectType, EffectType.TINT),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 21}", CheckRecord.class).effectType, EffectType.FILL),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 22}", CheckRecord.class).effectType, EffectType.STROKE),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 23}", CheckRecord.class).effectType, EffectType.TRITONE),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 24}", CheckRecord.class).effectType, EffectType.PRO_LEVELS),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 25}", CheckRecord.class).effectType, EffectType.DROP_SHADOW),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 26}", CheckRecord.class).effectType, EffectType.RADIAL_WIPE),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 27}", CheckRecord.class).effectType, EffectType.DISPLACEMENT_MAP),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 28}", CheckRecord.class).effectType, EffectType.MATTE3),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 29}", CheckRecord.class).effectType, EffectType.GAUSSIAN_BLUR),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 31}", CheckRecord.class).effectType, EffectType.MESH_WARP),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 32}", CheckRecord.class).effectType, EffectType.WAVY),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 33}", CheckRecord.class).effectType, EffectType.SPHERIZE),
                () -> assertEquals(mapper.readValue("{\"ty-e\": 34}", CheckRecord.class).effectType, EffectType.PUPPET),

                () -> assertEquals(mapper.readValue("{\"r\": 1}", CheckRecord.class).fillRule, FillRule.NON_ZERO),
                () -> assertEquals(mapper.readValue("{\"r\": 2}", CheckRecord.class).fillRule, FillRule.EVEN_ODD),

                () -> assertEquals(mapper.readValue("{\"t\": 1}", CheckRecord.class).gradientType, GradientType.LINEAR),
                () -> assertEquals(mapper.readValue("{\"t\": 2}", CheckRecord.class).gradientType, GradientType.RADIAL),

                () -> assertEquals(mapper.readValue("{\"ty-l\": 0}", CheckRecord.class).layerType, LayerType.PRECOMPOSITION),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 1}", CheckRecord.class).layerType, LayerType.SOLD_COLOR),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 2}", CheckRecord.class).layerType, LayerType.IMAGE),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 3}", CheckRecord.class).layerType, LayerType.NULL),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 4}", CheckRecord.class).layerType, LayerType.SHAPE),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 5}", CheckRecord.class).layerType, LayerType.TEXT),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 6}", CheckRecord.class).layerType, LayerType.AUDIO),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 7}", CheckRecord.class).layerType, LayerType.VIDEO_PLACEHOLDER),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 8}", CheckRecord.class).layerType, LayerType.IMAGE_SEQUENCE),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 9}", CheckRecord.class).layerType, LayerType.VIDEO),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 10}", CheckRecord.class).layerType, LayerType.IMAGE_PLACEHOLDER),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 11}", CheckRecord.class).layerType, LayerType.GUIDE),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 12}", CheckRecord.class).layerType, LayerType.ADJUSTMENT),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 13}", CheckRecord.class).layerType, LayerType.CAMERA),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 14}", CheckRecord.class).layerType, LayerType.LIGHT),
                () -> assertEquals(mapper.readValue("{\"ty-l\": 15}", CheckRecord.class).layerType, LayerType.DATA),

                () -> assertEquals(mapper.readValue("{\"lc\": 1}", CheckRecord.class).lineCap, LineCap.BUTT),
                () -> assertEquals(mapper.readValue("{\"lc\": 2}", CheckRecord.class).lineCap, LineCap.ROUND),
                () -> assertEquals(mapper.readValue("{\"lc\": 3}", CheckRecord.class).lineCap, LineCap.SQUARE),

                () -> assertEquals(mapper.readValue("{\"lj\": 1}", CheckRecord.class).lineJoin, LineJoin.MITER),
                () -> assertEquals(mapper.readValue("{\"lj\": 2}", CheckRecord.class).lineJoin, LineJoin.ROUND),
                () -> assertEquals(mapper.readValue("{\"lj\": 3}", CheckRecord.class).lineJoin, LineJoin.BEVEL),

                () -> assertEquals(mapper.readValue("{\"tt\": 0}", CheckRecord.class).matteMode, MatteMode.NORMAL),
                () -> assertEquals(mapper.readValue("{\"tt\": 1}", CheckRecord.class).matteMode, MatteMode.ALPHA),
                () -> assertEquals(mapper.readValue("{\"tt\": 2}", CheckRecord.class).matteMode, MatteMode.INVERTED_ALPHA),
                () -> assertEquals(mapper.readValue("{\"tt\": 3}", CheckRecord.class).matteMode, MatteMode.LUMA),
                () -> assertEquals(mapper.readValue("{\"tt\": 4}", CheckRecord.class).matteMode, MatteMode.INVERTED_LUMA),

                () -> assertEquals(mapper.readValue("{\"mm\": 1}", CheckRecord.class).mergeMode, MergeMode.NORMAL),
                () -> assertEquals(mapper.readValue("{\"mm\": 2}", CheckRecord.class).mergeMode, MergeMode.ADD),
                () -> assertEquals(mapper.readValue("{\"mm\": 3}", CheckRecord.class).mergeMode, MergeMode.SUBTRACT),
                () -> assertEquals(mapper.readValue("{\"mm\": 4}", CheckRecord.class).mergeMode, MergeMode.INTERSECT),
                () -> assertEquals(mapper.readValue("{\"mm\": 5}", CheckRecord.class).mergeMode, MergeMode.EXCLUDE),

                () -> assertEquals(mapper.readValue("{\"sy\": 1}", CheckRecord.class).starType, StarType.STAR),
                () -> assertEquals(mapper.readValue("{\"sy\": 2}", CheckRecord.class).starType, StarType.POLYGON),

                () -> assertEquals(mapper.readValue("{\"n\": \"d\"}", CheckRecord.class).strokeDashType, StrokeDashType.DASH),
                () -> assertEquals(mapper.readValue("{\"n\": \"g\"}", CheckRecord.class).strokeDashType, StrokeDashType.GAP),
                () -> assertEquals(mapper.readValue("{\"n\": \"o\"}", CheckRecord.class).strokeDashType, StrokeDashType.OFFSET),

                () -> assertEquals(mapper.readValue("{\"m-t\": 1}", CheckRecord.class).trimMultipleShapes, TrimMultipleShapes.INDIVIDUALLY),
                () -> assertEquals(mapper.readValue("{\"m-t\": 2}", CheckRecord.class).trimMultipleShapes, TrimMultipleShapes.SIMULTANEOUSLY)
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
