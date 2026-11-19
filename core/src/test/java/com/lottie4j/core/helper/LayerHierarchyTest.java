package com.lottie4j.core.helper;

import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.layer.Layer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerHierarchyTest {

    private static List<Layer> layers(String... layers) {
        return ObjectMapperFactory.getInstance().readValue("{\"v\":\"5\",\"fr\":30,\"ip\":0,\"op\":30,\"w\":10,\"h\":10,"
                + "\"layers\":[" + String.join(",", layers) + "]}", Animation.class).layers();
    }

    private static String layer(String name, int index, String more) {
        return "{\"ty\":4,\"nm\":\"" + name + "\",\"ind\":" + index + (more.isEmpty() ? "" : "," + more) + "}";
    }

    @Test
    void ancestorsComeOutermostFirst() {
        List<Layer> layers = layers(layer("child", 1, "\"parent\":2"), layer("parent", 2, "\"parent\":3"),
                layer("grandparent", 3, ""));

        List<Layer> chain = LayerHierarchy.parentChain(layers.get(0), LayerHierarchy.byIndex(layers));

        assertEquals(List.of("grandparent", "parent"), chain.stream().map(Layer::name).toList());
    }

    @Test
    void aChainEndsAtAParentThatDoesNotExist() {
        List<Layer> layers = layers(layer("child", 1, "\"parent\":2"), layer("parent", 2, "\"parent\":99"));

        List<Layer> chain = LayerHierarchy.parentChain(layers.get(0), LayerHierarchy.byIndex(layers));

        assertEquals(List.of("parent"), chain.stream().map(Layer::name).toList());
    }

    @Test
    void parentsThatRunInACircleCountAsNoParent() {
        List<Layer> layers = layers(layer("a", 1, "\"parent\":2"), layer("b", 2, "\"parent\":3"),
                layer("c", 3, "\"parent\":1"), layer("self", 4, "\"parent\":4"));
        Map<Integer, Layer> byIndex = LayerHierarchy.byIndex(layers);

        for (Layer layer : layers) {
            assertTrue(LayerHierarchy.parentChain(layer, byIndex).isEmpty(), layer.name());
        }
    }

    @Test
    void ofTwoLayersWithTheSameIndexTheFirstCounts() {
        List<Layer> layers = layers(layer("first", 1, ""), layer("second", 1, ""));

        assertSame(layers.get(0), LayerHierarchy.byIndex(layers).get(1));
    }

    @Test
    void theMatteIsTheLayerAboveIfThatIsMarkedAsOne() {
        List<Layer> layers = layers(layer("matte", 1, "\"td\":1"), layer("content", 2, "\"tt\":1"),
                layer("plain", 3, ""), layer("no matte above", 4, "\"tt\":1"));
        Map<Integer, Layer> byIndex = LayerHierarchy.byIndex(layers);

        assertSame(layers.get(0), LayerHierarchy.matteSource(layers, 1, byIndex));
        assertNull(LayerHierarchy.matteSource(layers, 2, byIndex));
        assertNull(LayerHierarchy.matteSource(layers, 3, byIndex));
        assertNull(LayerHierarchy.matteSource(layers, 0, byIndex));
    }

    @Test
    void aLayerMayNameItsMatte() {
        List<Layer> layers = layers(layer("content", 1, "\"tt\":1,\"tp\":3"), layer("in between", 2, "\"td\":1"),
                layer("named matte", 3, ""), layer("names itself", 4, "\"tt\":1,\"tp\":4"),
                layer("names nobody", 5, "\"tt\":1,\"tp\":42"));
        Map<Integer, Layer> byIndex = LayerHierarchy.byIndex(layers);

        assertSame(layers.get(2), LayerHierarchy.matteSource(layers, 0, byIndex));
        assertNull(LayerHierarchy.matteSource(layers, 3, byIndex));
        assertNull(LayerHierarchy.matteSource(layers, 4, byIndex));
        assertTrue(LayerHierarchy.matteSources(layers, byIndex).contains(layers.get(2)));
        assertEquals(1, LayerHierarchy.matteSources(layers, byIndex).size());
    }
}
