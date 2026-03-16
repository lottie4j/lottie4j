package com.lottie4j.core.model;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.dot.Manifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DotTest {
    private static final ObjectMapper mapper = ObjectMapperFactory.getInstance();

    @Test
    void shouldParseManifest() throws JacksonException {
        var json = """
                {
                    "version":"2",
                    "generator":"@dotlottie/dotlottie-js@1.4.0",
                    "animations":[
                        {
                            "id":"animation"
                        }
                    ]
                }
                """;

        var manifest = mapper.readValue(json, Manifest.class);

        assertAll(
                () -> assertNotNull(manifest),
                () -> assertEquals("2", manifest.version()),
                () -> assertEquals("@dotlottie/dotlottie-js@1.4.0", manifest.generator()),
                () -> assertEquals(1, manifest.animations().size()),
                () -> assertEquals("animation", manifest.animations().get(0).id())
        );
    }
}
