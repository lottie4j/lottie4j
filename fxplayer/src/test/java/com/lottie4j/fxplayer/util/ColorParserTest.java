package com.lottie4j.fxplayer.util;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ColorParserTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ColorParserTest.class);

    @Test
    void parsesRgbHexWithAndWithoutHash() {
        Color withHash = ColorParser.parse("#3366CC");
        Color withoutHash = ColorParser.parse("3366CC");

        assertEquals(Color.color(0x33 / 255.0, 0x66 / 255.0, 0xCC / 255.0), withHash);
        assertEquals(Color.color(0x33 / 255.0, 0x66 / 255.0, 0xCC / 255.0), withoutHash);
    }

    @Test
    void parsesRgbaHex() {
        Color color = ColorParser.parse("11223380");

        assertEquals(Color.color(0x11 / 255.0, 0x22 / 255.0, 0x33 / 255.0, 0x80 / 255.0), color);
    }

    @Test
    void returnsNullForInvalidInput() {
        assertNull(ColorParser.parse("#12345"));
        assertNull(ColorParser.parse("#GGGGGG"));
    }
}

