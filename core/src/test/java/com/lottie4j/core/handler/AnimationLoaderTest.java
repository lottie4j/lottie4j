package com.lottie4j.core.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class AnimationLoaderTest {

    private static final ObjectMapper mapper = new ObjectMapper();


    @Test
    void testFileToObjectToJson() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lottie_file/timeline_animation.json").getFile());
        String jsonFromFile = LottieFileLoader.loadAsString(f);
        var animation = mapper.readValue(jsonFromFile, Animation.class);
        ObjectMapper mapper = new ObjectMapper();
        String jsonFromObject = mapper.writeValueAsString(animation);

        System.out.println("Original:\n" + jsonFromFile.replace("\n", "").replace(" ", ""));
        System.out.println("Generated:\n" + jsonFromObject);

        assertAll(
                () -> assertInstanceOf(Animation.class, animation),
                () -> assertEquals(0, animation.has3dLayers()),
                () -> assertEquals(30, animation.framesPerSecond()),
                () -> assertEquals(375, animation.height()),
                () -> assertEquals(375, animation.width()),
                () -> assertEquals(0, animation.inPoint()),
                () -> assertEquals(31, animation.outPoint()),
                () -> assertEquals("Timeline", animation.name()),
                () -> assertEquals("5.7.14", animation.version()),
                () -> assertEquals(7, animation.assets().size()),
                () -> assertEquals(8, animation.layers().size())
        );

        layerCheck(animation.layers().get(0), "line_7-composition", 0);
        layerCheck(animation.layers().get(1), "star_6-composition", 0);
        layerCheck(animation.layers().get(2), "polygon_5-composition", 0);
        layerCheck(animation.layers().get(3), "triangle_4-composition", 0);
        layerCheck(animation.layers().get(4), "circle_3-composition", 0);
        layerCheck(animation.layers().get(5), "rounded_2-composition", 0);
        layerCheck(animation.layers().get(6), "rectangle_1-composition", 0);
        layerCheck(animation.layers().get(7), "Scene-background", 1);
    }

    private void layerCheck(Layer layer, String name, int numberOfShapes) {
        assertAll(
                () -> assertEquals(name, layer.name()),
                () -> assertEquals(numberOfShapes, layer.shapes() == null ? 0 : layer.shapes().size())
        );
    }
}
