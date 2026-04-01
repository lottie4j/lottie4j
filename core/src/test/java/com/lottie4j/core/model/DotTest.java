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
    void shouldParseV1Manifest() throws JacksonException {
        var json = """
                {
                    "version":"1",
                    "generator":"LottieFiles",
                    "author":"Jane Doe",
                    "animations":[
                        {
                            "id":"animation",
                            "autoplay":true,
                            "loop":true,
                            "speed":1,
                            "direction":1,
                            "themeColor":"#FF0000"
                        }
                    ]
                }
                """;

        var manifest = mapper.readValue(json, Manifest.class);

        assertAll(
                () -> assertNotNull(manifest),
                () -> assertEquals("1", manifest.version()),
                () -> assertEquals("LottieFiles", manifest.generator()),
                () -> assertEquals("Jane Doe", manifest.author()),
                () -> assertEquals(1, manifest.animations().size()),
                () -> assertEquals("animation", manifest.animations().get(0).id()),
                () -> assertTrue(manifest.animations().get(0).autoplay()),
                () -> assertTrue(manifest.animations().get(0).loop()),
                () -> assertEquals(1.0, manifest.animations().get(0).speed()),
                () -> assertEquals(1, manifest.animations().get(0).direction()),
                () -> assertEquals("#FF0000", manifest.animations().get(0).themeColor()),
                // v2-only fields absent
                () -> assertNull(manifest.themes()),
                () -> assertNull(manifest.stateMachines()),
                () -> assertNull(manifest.initial())
        );
    }

    @Test
    void shouldParseV2Manifest() throws JacksonException {
        var json = """
                {
                    "version":"2",
                    "generator":"@dotlottie/dotlottie-js@1.4.0",
                    "initial":{
                        "animation":"animation_02"
                    },
                    "animations":[
                        {
                            "id":"animation_01"
                        },
                        {
                            "id":"animation_02",
                            "initialTheme":"theme_01",
                            "background":"#F5F5F5",
                            "themes":["theme_01","theme_02"]
                        }
                    ],
                    "themes":[
                        {"id":"theme_01","name":"Light Theme"},
                        {"id":"theme_02","name":"Dark Theme"}
                    ],
                    "stateMachines":[
                        {"id":"sm_01","name":"Button States"}
                    ]
                }
                """;

        var manifest = mapper.readValue(json, Manifest.class);

        assertAll(
                () -> assertNotNull(manifest),
                () -> assertEquals("2", manifest.version()),
                () -> assertEquals("@dotlottie/dotlottie-js@1.4.0", manifest.generator()),
                // v1-only author absent
                () -> assertNull(manifest.author()),
                // animations
                () -> assertEquals(2, manifest.animations().size()),
                () -> assertEquals("animation_01", manifest.animations().get(0).id()),
                () -> assertEquals("animation_02", manifest.animations().get(1).id()),
                () -> assertEquals("theme_01", manifest.animations().get(1).initialTheme()),
                () -> assertEquals("#F5F5F5", manifest.animations().get(1).background()),
                () -> assertEquals(2, manifest.animations().get(1).themes().size()),
                // themes
                () -> assertEquals(2, manifest.themes().size()),
                () -> assertEquals("theme_01", manifest.themes().get(0).id()),
                () -> assertEquals("Light Theme", manifest.themes().get(0).name()),
                // stateMachines
                () -> assertEquals(1, manifest.stateMachines().size()),
                () -> assertEquals("sm_01", manifest.stateMachines().get(0).id()),
                () -> assertEquals("Button States", manifest.stateMachines().get(0).name()),
                // initial
                () -> assertNotNull(manifest.initial()),
                () -> assertEquals("animation_02", manifest.initial().animation()),
                () -> assertNull(manifest.initial().stateMachine())
        );
    }

    /** Legacy test kept for regression. */
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
