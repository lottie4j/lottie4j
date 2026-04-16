package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.shape.style.Fill;
import com.lottie4j.fxplayer.util.FxTestHelper;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FillStyle.
 * Validates fill color resolution and opacity handling.
 */
public class FillStyleBasicTest {

    @BeforeAll
    public static void initToolkit() {
        FxTestHelper.initToolkit();
    }

    @Test
    void nullFillReturnsBlack() {
        FillStyle fillStyle = new FillStyle(null);
        Color color = fillStyle.getColor(0.0);
        assertEquals(Color.BLACK, color);
    }

    @Test
    void nullColorReturnsBlack() {
        Fill fill = new Fill(null, null, null, null, null, null, null, null, null, null, null, null);
        FillStyle fillStyle = new FillStyle(fill);
        Color color = fillStyle.getColor(0.0);
        assertEquals(Color.BLACK, color);
    }

    @Test
    void fillStyleCanBeInstantiated() {
        Fill fill = new Fill(null, null, null, null, null, null, null, null, null, null, null, null);
        FillStyle fillStyle = new FillStyle(fill);
        assertNotNull(fillStyle);
    }

    @Test
    void multipleFramesReturnColors() {
        Fill fill = new Fill(null, null, null, null, null, null, null, null, null, null, null, null);
        FillStyle fillStyle = new FillStyle(fill);
        
        Color color1 = fillStyle.getColor(0.0);
        Color color2 = fillStyle.getColor(10.0);
        Color color3 = fillStyle.getColor(20.0);
        
        assertNotNull(color1);
        assertNotNull(color2);
        assertNotNull(color3);
    }

    @Test
    void getColorReturnsValidColor() {
        Fill fill = new Fill(null, null, null, null, null, null, null, null, null, null, null, null);
        FillStyle fillStyle = new FillStyle(fill);
        
        Color color = fillStyle.getColor(0.0);
        
        assertNotNull(color);
        assertEquals(Color.BLACK, color);
    }
}

