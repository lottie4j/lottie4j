package com.lottie4j.core.file;

import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.dot.DotLottie;
import com.lottie4j.core.model.dot.Manifest;
import com.lottie4j.core.model.dot.ManifestAnimation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * What is written must be what {@link LottieFileLoader} reads back.
 */
class LottieFileSaverTest {

    private static final String JSON = """
            {"v": "5.7.0", "nm": "Saved", "fr": 30, "ip": 0, "op": 60, "w": 100, "h": 50, "layers": [
              {"ty": 4, "nm": "Shape", "ind": 1, "ip": 0, "op": 60, "st": 0, "shapes": []}]}""";

    @TempDir
    Path directory;

    private static Animation animation() throws LottieFileException {
        return LottieFileLoader.loadJson(JSON);
    }

    @Test
    void jsonIsReadBackAsWritten() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        LottieFileSaver.save(animation(), out);

        assertEquals(animation(), LottieFileLoader.load(new ByteArrayInputStream(out.toByteArray())));
    }

    @Test
    void jsonFileIsReadBackAsWritten() throws Exception {
        File file = directory.resolve("saved.json").toFile();
        LottieFileSaver.save(animation(), file);

        assertEquals(animation(), LottieFileLoader.load(file));
    }

    @Test
    void oneAnimationBecomesAnArchiveOfVersion2() throws Exception {
        File file = directory.resolve("saved.lottie").toFile();
        try (var out = Files.newOutputStream(file.toPath())) {
            LottieFileSaver.saveDotLottie(animation(), "saved", out);
        }

        DotLottie read = LottieFileLoader.loadDotLottie(file);
        assertEquals("2", read.manifest().version());
        assertEquals("saved", read.manifest().animations().get(0).id());
        assertEquals(List.of(animation()), read.animations());
        // the loader for a single animation takes the archive as well
        assertEquals(animation(), LottieFileLoader.load(file));
    }

    @Test
    void archiveKeepsItsThemesImagesAndFonts() throws Exception {
        Manifest manifest = new Manifest("2", "test", null, List.of(
                new ManifestAnimation("first", null, null, null, null, null, null, null, null),
                new ManifestAnimation("second", null, null, null, null, null, null, null, null)), null, null, null);
        DotLottie written = new DotLottie(manifest, List.of(animation(), animation()),
                Map.of("dark", "{}".getBytes()), Map.of("machine", "{}".getBytes()),
                Map.of("image.png", new byte[]{1, 2, 3}), Map.of("font.ttf", new byte[]{4, 5}));
        File file = directory.resolve("full.lottie").toFile();
        LottieFileSaver.saveDotLottie(written, file);

        DotLottie read = LottieFileLoader.loadDotLottie(file);
        assertEquals(2, read.animations().size());
        assertArrayEquals("{}".getBytes(), read.themes().get("dark"));
        assertArrayEquals("{}".getBytes(), read.stateMachines().get("machine"));
        assertArrayEquals(new byte[]{1, 2, 3}, read.imageAssets().get("image.png"));
        assertArrayEquals(new byte[]{4, 5}, read.fontAssets().get("font.ttf"));
    }

    @Test
    void version1GetsTheDirectoriesOfVersion1() throws Exception {
        Manifest manifest = new Manifest("1.0", "test", "someone", List.of(
                new ManifestAnimation("only", true, true, 1.0, 1, null, null, null, null)), null, null, null);
        File file = directory.resolve("legacy.lottie").toFile();
        LottieFileSaver.saveDotLottie(new DotLottie(manifest, List.of(animation()), null, null, null, null), file);

        try (var zip = new java.util.zip.ZipFile(file)) {
            assertEquals(List.of("manifest.json", "animations/only.json"),
                    zip.stream().map(java.util.zip.ZipEntry::getName).toList());
        }
        assertEquals(List.of(animation()), LottieFileLoader.loadDotLottie(file).animations());
    }

    @Test
    void anArchiveWithoutAUsableManifestIsNotWritten() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Manifest unsafe = new Manifest("2", "test", null, List.of(
                new ManifestAnimation("../outside", null, null, null, null, null, null, null, null)), null, null,
                null);

        assertThrows(LottieFileException.class, () -> LottieFileSaver.saveDotLottie(
                new DotLottie(null, List.of(animation()), null, null, null, null), out));
        assertThrows(LottieFileException.class, () -> LottieFileSaver.saveDotLottie(
                new DotLottie(unsafe, List.of(animation()), null, null, null, null), out));
        assertThrows(LottieFileException.class, () -> LottieFileSaver.saveDotLottie(
                new DotLottie(unsafe, List.of(), null, null, null, null), out));
    }
}
