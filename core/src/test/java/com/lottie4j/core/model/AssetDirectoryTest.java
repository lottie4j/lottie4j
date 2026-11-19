package com.lottie4j.core.model;

import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.asset.Asset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The {@code u} of an image asset is the directory of an externally stored file ({@code "images/"}),
 * a plain string. It used to be read as a shape type, which made every file with an external image
 * fail to load.
 */
class AssetDirectoryTest {

    private static Asset read(String json) {
        return ObjectMapperFactory.getInstance().readValue(json, Asset.class);
    }

    @Test
    void externalImageHasADirectoryAndAFileName() {
        Asset asset = read("""
                {"id": "image_0", "w": 4, "h": 2, "u": "images/", "p": "img_0.png", "e": 0}""");

        assertEquals("images/", asset.directory());
        assertEquals("img_0.png", asset.fileName());
        assertEquals(0, asset.embedded());
    }

    @Test
    void embeddedImageHasAnEmptyDirectory() {
        Asset asset = read("""
                {"id": "image_0", "w": 4, "h": 2, "u": "", "p": "data:image/png;base64,AAAA", "e": 1}""");

        assertEquals("", asset.directory());
        assertEquals(1, asset.embedded());
    }

    @Test
    void directoryIsOptional() {
        assertNull(read("""
                {"id": "comp_0", "layers": []}""").directory());
    }
}
