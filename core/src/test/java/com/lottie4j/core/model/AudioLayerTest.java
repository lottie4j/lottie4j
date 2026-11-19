package com.lottie4j.core.model;

import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.layer.Layer;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * An audio layer ({@code ty} 6) carries its levels in {@code au.lv}: the left and the right
 * channel in percent.
 */
class AudioLayerTest {

    private static final String AUDIO_LAYER = """
            {"ty": 6, "nm": "Sound", "refId": "audio_0", "ip": 0, "op": 60, "st": 0,
             "au": {"lv": {"a": 0, "k": [80, 60]}}}""";

    private static Layer read(String json) {
        return ObjectMapperFactory.getInstance().readValue(json, Layer.class);
    }

    @Test
    void audioLevelsAreRead() {
        Layer layer = read(AUDIO_LAYER);

        assertEquals(LayerType.AUDIO, layer.layerType());
        assertEquals("audio_0", layer.referenceId());
        assertNotNull(layer.audioSettings());
        assertNotNull(layer.audioSettings().level());
        assertEquals(2, layer.audioSettings().level().keyframes().size());
    }

    @Test
    void audioLevelsAreWrittenBack() {
        var mapper = ObjectMapperFactory.getInstance();
        JsonNode written = mapper.readTree(mapper.writeValueAsString(read(AUDIO_LAYER)));

        assertEquals(mapper.readTree(AUDIO_LAYER).get("au"), written.get("au"));
    }

    @Test
    void otherLayersHaveNoAudioSettings() {
        assertNull(read("""
                {"ty": 4, "nm": "Shape", "shapes": []}""").audioSettings());
    }
}
