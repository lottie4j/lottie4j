package com.lottie4j.core.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.keyframe.NumberKeyframe;
import com.lottie4j.core.model.keyframe.TimedKeyframe;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.*;

class KeyframeTest {

    private static final ObjectMapper mapper = ObjectMapperFactory.getInstance();

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
                () -> assertInstanceOf(NumberKeyframe.class, animated.keyframes().get(0)),
                () -> assertEquals(123, ((NumberKeyframe) animated.keyframes().get(0)).intValue()),
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
                () -> assertInstanceOf(NumberKeyframe.class, animated.keyframes().get(0)),
                () -> assertEquals(128, ((NumberKeyframe) animated.keyframes().get(0)).intValue()),
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
                () -> assertInstanceOf(NumberKeyframe.class, animated.keyframes().get(0)),
                () -> assertEquals(5.01, ((NumberKeyframe) animated.keyframes().get(0)).doubleValue()),
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
                () -> assertInstanceOf(TimedKeyframe.class, animated.keyframes().get(0)),
                () -> assertEquals(60.0, ((TimedKeyframe) animated.keyframes().get(0)).time()),
                () -> JSONAssert.assertEquals(json, mapper.writeValueAsString(animated), false)
        );
    }
}
