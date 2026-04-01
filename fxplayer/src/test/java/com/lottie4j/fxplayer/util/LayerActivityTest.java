package com.lottie4j.fxplayer.util;

import com.lottie4j.core.model.layer.Layer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LayerActivityTest {

    @Test
    void evaluatesLayerAsActiveWithinInclusiveExclusiveBounds() {
        Layer layer = layer(10, 20);

        assertFalse(LayerActivity.isActiveAtFrame(layer, 9.99));
        assertTrue(LayerActivity.isActiveAtFrame(layer, 10.0));
        assertTrue(LayerActivity.isActiveAtFrame(layer, 19.999));
        assertFalse(LayerActivity.isActiveAtFrame(layer, 20.0));
    }

    @Test
    void defaultsToAlwaysActiveWhenInOutPointsAreMissing() {
        Layer layer = layer(null, null);

        assertTrue(LayerActivity.isActiveAtFrame(layer, 0.0));
        assertTrue(LayerActivity.isActiveAtFrame(layer, 10_000.0));
    }

    @Test
    void respectsMissingOutPointAsInfinity() {
        Layer layer = layer(5, null);

        assertFalse(LayerActivity.isActiveAtFrame(layer, 4.999));
        assertTrue(LayerActivity.isActiveAtFrame(layer, 5.0));
        assertTrue(LayerActivity.isActiveAtFrame(layer, 9999.0));
    }

    @Test
    void respectsMissingInPointAsZero() {
        Layer layer = layer(null, 8);

        assertFalse(LayerActivity.isActiveAtFrame(layer, -0.001));
        assertTrue(LayerActivity.isActiveAtFrame(layer, 0.0));
        assertFalse(LayerActivity.isActiveAtFrame(layer, 8.0));
    }

    private static Layer layer(Integer inPoint, Integer outPoint) {
        return new Layer(
                null, null, null, null, null, null, null,
                null, inPoint, outPoint, null,
                null, null, null, null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, null, null, null, null
        );
    }
}
