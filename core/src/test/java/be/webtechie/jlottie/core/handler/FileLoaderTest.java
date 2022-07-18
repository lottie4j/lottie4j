package be.webtechie.jlottie.core.handler;

import be.webtechie.jlottie.core.model.Animation;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class FileLoaderTest {

    @Test
    void testLoadFile() throws IOException {
        File f = new File(this.getClass().getResource("/lottie/lf20_gOmta2.json").getFile());
        Animation a = FileLoader.loadFile(f);
        assertAll(
                () -> assertNotNull(a),
                () -> assertEquals("5.5.7", a.version())
        );
    }
}
