package com.lottie4j.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.model.keyframe.Keyframe;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KeyframeTest {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testSingleValue() throws JsonProcessingException {
        var json = """
                {
                    "k": 123
                }
                """;

        var animated = mapper.readValue(json, Animated.class);

        assertAll(
                () -> assertEquals(1, animated.keyframes().size()),
                () -> assertTrue(animated.keyframes().get(0) instanceof NumberKeyframe),
                () -> assertEquals(123, ((NumberKeyframe) animated.keyframes().get(0)).value().intValue()),
                () -> JSONAssert.assertEquals(json, mapper.writeValueAsString(animated), false)
        );
    }

    @Test
    void testInteger() throws JsonProcessingException {
        var json = """
                {
                    "k": [
                      128,
                      256
                    ]
                }
                """;

        var animated = mapper.readValue(json, Animated.class);

        assertAll(
                () -> assertEquals(2, animated.keyframes().size()),
                () -> assertTrue(animated.keyframes().get(0) instanceof NumberKeyframe),
                () -> assertEquals(128, ((NumberKeyframe) animated.keyframes().get(0)).value().intValue()),
                () -> JSONAssert.assertEquals(json, mapper.writeValueAsString(animated), false)
        );
    }

    @Test
    void testDouble() throws JsonProcessingException {
        var json = """
                {
                    "k": [
                      5.01,
                      6.02
                    ]
                }
                """;

        var animated = mapper.readValue(json, Animated.class);

        assertAll(
                () -> assertEquals(2, animated.keyframes().size()),
                () -> assertTrue(animated.keyframes().get(0) instanceof NumberKeyframe),
                () -> assertEquals(5.01, ((NumberKeyframe) animated.keyframes().get(0)).value().doubleValue()),
                () -> JSONAssert.assertEquals(json, mapper.writeValueAsString(animated), false)
        );
    }

    @Test
    void testTimed() throws JsonProcessingException {
        var json = """
                {
                    "k": [
                     {
                       "i": {
                         "x": [
                           0.833
                         ],
                         "y": [
                           0.833
                         ]
                       },
                       "o": {
                         "x": [
                           0.167
                         ],
                         "y": [
                           0.167
                         ]
                       },
                       "t": 60,
                       "s": [
                         1.1,
                         2.2,
                         3.3
                       ]
                     },
                     {
                       "t": 60,
                       "s": [
                         360.0
                       ]
                     }
                   ]
                }
                """;

        var animated = mapper.readValue(json, Animated.class);

        assertAll(
                () -> assertEquals(2, animated.keyframes().size()),
                () -> assertTrue(animated.keyframes().get(0) instanceof TimedKeyframe),
                () -> assertEquals(60, ((TimedKeyframe) animated.keyframes().get(0)).time()),
                () -> JSONAssert.assertEquals(json, mapper.writeValueAsString(animated), false)
        );
    }

    @Test
    @Disabled("Not sure if this use-case exists in Lottie format, to be checked later")
    void testMixed() throws JsonProcessingException {
        var json = """
                {
                    "k": [
                    100,
                    33.44,
                     {
                       "t": 60,
                       "s": [
                         1.1,
                         2.2,
                         3.3
                       ]
                     }
                   ]
                }
                """;

        var keyFrames = mapper.readValue(json, new TypeReference<List<Keyframe>>() {
        });

        assertAll(
                () -> assertEquals(3, keyFrames.size()),
                () -> assertTrue(keyFrames.get(0) instanceof NumberKeyframe),
                () -> assertTrue(keyFrames.get(1) instanceof NumberKeyframe),
                () -> assertTrue(keyFrames.get(2) instanceof TimedKeyframe),
                () -> JSONAssert.assertEquals(json, mapper.writeValueAsString(keyFrames), false)
        );
    }
}
