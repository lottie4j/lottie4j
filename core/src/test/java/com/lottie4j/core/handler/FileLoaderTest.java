package com.lottie4j.core.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class FileLoaderTest {

    private static final ObjectMapper mapper = new ObjectMapper();


    @Test
    void testLoadSingleLayerFileNoShapes() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lottie_file/java_duke_single_layer_no_shapes.json").getFile());
        var l = mapper.readValue(FileLoader.loadFileAsString(f), Layer.class);
        assertAll(
                () -> assertNotNull(l),
                () -> assertEquals(3, l.transform().anchor().keyframes().size())
                //() -> assertEquals(1, l.transform().position().x().keyframes().size())
        );
    }

    @Test
    void testLoadSingleLayerFile() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lottie_file/java_duke_single_layer.json").getFile());
        var l = mapper.readValue(FileLoader.loadFileAsString(f), Layer.class);
        assertAll(
                () -> assertNotNull(l),
                () -> assertEquals(3, l.transform().anchor().keyframes().size())
                //() -> assertEquals(1, l.transform().position().x().keyframes().size())
        );
    }

    @Test
    void testLoadSmallFile() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lottie_file/java_duke.json").getFile());
        var a = mapper.readValue(FileLoader.loadFileAsString(f), Animation.class);
        assertAll(
                () -> assertNotNull(a),
                () -> assertEquals("5.1.20", a.version()),
                () -> assertEquals(0, a.inPoint()),
                () -> assertEquals(38, a.outPoint()),
                () -> assertEquals(550, a.width()),
                () -> assertEquals(400, a.height()),
                () -> assertEquals(5, a.layers().size()),
                () -> assertEquals("Java_Duke_waving", a.layers().get(0).name())
        );
    }

    @Test
    void testLoadBigFile() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lottie_file/lf20_gOmta2.json").getFile());
        var a = mapper.readValue(FileLoader.loadFileAsString(f), Animation.class);
        assertAll(
                () -> assertNotNull(a),
                () -> assertEquals("5.5.7", a.version())
        );
    }
}
