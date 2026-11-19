package com.lottie4j.core.helper;

import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.keyframe.TimedKeyframe;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Before version 5, Bodymovin wrote the end value of a segment into its keyframe ({@code e}) and
 * closed the list with a keyframe that has a time only.
 */
class OldFormatKeyframesTest {

    private static final String OLD = "{\"a\":1,\"k\":[{\"t\":0,\"s\":[0],\"e\":[90]},"
            + "{\"t\":30,\"s\":[90],\"e\":[360]},{\"t\":60}]}";

    private static <T> T read(String json, Class<T> type) {
        return ObjectMapperFactory.getInstance().readValue(json, type);
    }

    @Test
    void theLastKeyframeOfAnOldFileEndsOnTheEndValueOfTheOneBefore() {
        Animated rotation = read(OLD, Animated.class);

        assertEquals(45.0, rotation.getValue(0, 15), 1e-9);
        assertEquals(225.0, rotation.getValue(0, 45), 1e-9);
        assertEquals(360.0, rotation.getValue(0, 60), 1e-9);
        assertEquals(360.0, rotation.getValue(0, 70), 1e-9);
    }

    @Test
    void theEndValueIsStillThere() {
        TimedKeyframe second = (TimedKeyframe) read(OLD, Animated.class).keyframes().get(1);

        assertEquals(360, second.endValues().get(0).intValue());
    }

    @Test
    void aKeyframeWithoutEndValueBeforeItKeepsThePreviousValue() {
        Animated value = read("{\"a\":1,\"k\":[{\"t\":0,\"s\":[7]},{\"t\":30}]}", Animated.class);

        assertEquals(7.0, value.getValue(0, 30), 1e-9);
    }

    @Test
    void keyframesInTheCurrentFormAreLeftAlone() {
        Animated value = read("{\"a\":1,\"k\":[{\"t\":0,\"s\":[0]},{\"t\":30,\"s\":[10]}]}", Animated.class);

        assertSame(value.keyframes(), OldFormatKeyframes.complete(value.keyframes()));
        assertNull(((TimedKeyframe) value.keyframes().get(0)).endValues());
        assertEquals(5.0, value.getValue(0, 15), 1e-9);
    }

    @Test
    void theLastShapeOfAnOldPathIsTheEndShapeOfTheOneBefore() {
        String start = "{\"c\":true,\"v\":[[0,0],[10,0]],\"i\":[[0,0],[0,0]],\"o\":[[0,0],[0,0]]}";
        String end = "{\"c\":true,\"v\":[[0,0],[20,0]],\"i\":[[0,0],[0,0]],\"o\":[[0,0],[0,0]]}";
        AnimatedBezier path = read("{\"a\":1,\"k\":[{\"t\":0,\"s\":[" + start + "],\"e\":[" + end + "]},{\"t\":30}]}",
                AnimatedBezier.class);

        assertEquals(2, path.beziers().size());
        assertEquals(path.beziers().get(0).endBeziers(), path.beziers().get(1).beziers());
    }
}
