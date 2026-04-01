package com.lottie4j.core.file;

import tools.jackson.databind.ObjectMapper;
import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.dot.DotLottie;
import com.lottie4j.core.model.dot.Manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Utility class for loading Lottie animation files from various sources.
 * Provides methods to deserialize JSON into {@link Animation} objects.
 */
public class LottieFileLoader {

    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getInstance();

    private static final String JSON_EXTENSION = ".json";
    private static final String DOT_LOTTIE_EXTENSION = ".lottie";

    /** Prefix for the animations directory (v2 spec). */
    private static final String DIR_ANIMATIONS = "a/";
    /** Prefix for the animations directory (v1 / legacy spec). */
    private static final String DIR_ANIMATIONS_LEGACY = "animations/";
    /** Prefix for the themes directory (v2). */
    private static final String DIR_THEMES = "t/";
    /** Prefix for the state machines directory (v2). */
    private static final String DIR_STATE_MACHINES = "s/";
    /** Prefix for the image assets directory (v2). */
    private static final String DIR_IMAGES = "i/";
    /** Prefix for the font assets directory (v2). */
    private static final String DIR_FONTS = "f/";

    /**
     * Prevents instantiation of this utility class.
     */
    private LottieFileLoader() {
        // Hide constructor
    }

    /**
     * Loads a Lottie animation from a file.
     * Supports plain `.json` Lottie files and `.lottie` dotLottie archives.
     * For dotLottie input, the first animation declared in the manifest is returned.
     *
     * @param file file containing Lottie data
     * @return deserialized animation
     * @throws LottieFileException if the file does not exist, the file type is unsupported,
     *                             or the content cannot be read or parsed
     */
    public static Animation load(File file) throws LottieFileException {
        if (!file.exists()) {
            throw new LottieFileException("file not found");
        }

        String fileName = file.getName().toLowerCase(Locale.ROOT);

        if (fileName.endsWith(JSON_EXTENSION)) {
            var animation = parseJsonFile(file);
            if (animation == null) {
                throw new LottieFileException("JSON file does not contain a valid animation");
            }
            return animation;
        }

        if (fileName.endsWith(DOT_LOTTIE_EXTENSION)) {
            var dotLottie = loadFromDottieFile(file);
            if (!dotLottie.animations().isEmpty()) {
                return dotLottie.animations().getFirst();
            }
            throw new LottieFileException("file does not contain any animations");
        }

        throw new LottieFileException("unrecognised file type");
    }

    /**
     * Loads a full dotLottie package from a `.lottie` archive file.
     *
     * @param file `.lottie` archive file
     * @return populated {@link DotLottie} containing manifest and all manifest-declared animations
     * @throws LottieFileException if the file does not exist, is not a `.lottie` file,
     *                             or the archive content is invalid or unreadable
     */
    public static DotLottie loadDotLottie(File file) throws LottieFileException {
        if (!file.exists()) {
            throw new LottieFileException("file not found");
        }

        String fileName = file.getName().toLowerCase(Locale.ROOT);

        if (fileName.endsWith(DOT_LOTTIE_EXTENSION)) {
            return loadFromDottieFile(file);
        }

        throw new LottieFileException("unrecognised file type");
    }

    /**
     * Reads a JSON file from disk and parses it as a single {@link Animation}.
     *
     * @param file JSON file to parse
     * @return parsed animation
     * @throws LottieFileException if reading or parsing fails
     */
    private static Animation parseJsonFile(File file) throws LottieFileException {
        try {
            return parseJsonString(Files.readString(file.toPath()));
        } catch (Exception e) {
            throw new LottieFileException("JSON could not be read: " + e.getMessage());
        }
    }

    /**
     * Parses a raw JSON string into an {@link Animation}.
     *
     * @param json raw Lottie JSON
     * @return parsed animation
     * @throws LottieFileException if parsing fails
     */
    private static Animation parseJsonString(String json) throws LottieFileException {
        try {
            return OBJECT_MAPPER.readValue(json, Animation.class);
        } catch (Exception e) {
            throw new LottieFileException("JSON could not be parsed: " + e.getMessage());
        }
    }

    /**
     * Parses a raw JSON string from a stream into an {@link Animation}.
     *
     * @param stream {@link InputStream}
     * @return parsed animation
     * @throws LottieFileException if parsing fails
     */
    private static Animation parseJsonString(InputStream stream) throws LottieFileException {
        try {
            return OBJECT_MAPPER.readValue(stream, Animation.class);
        } catch (Exception e) {
            throw new LottieFileException("JSON could not be parsed: " + e.getMessage());
        }
    }

    /**
     * Loads a dotLottie archive, parses {@code manifest.json}, resolves all animations listed
     * in the manifest, and collects any optional theme, state-machine, image, and font assets.
     *
     * @param file {@code .lottie} zip archive
     * @return populated dotLottie object
     * @throws LottieFileException if the archive is invalid or required entries are missing
     */
    private static DotLottie loadFromDottieFile(File file) throws LottieFileException {
        try (ZipFile zipFile = new ZipFile(file)) {
            ZipEntry manifestEntry = zipFile.getEntry("manifest.json");
            if (manifestEntry == null) {
                throw new LottieFileException("missing manifest.json");
            }

            Manifest manifest;
            try (InputStream manifestStream = zipFile.getInputStream(manifestEntry)) {
                manifest = OBJECT_MAPPER.readValue(manifestStream, Manifest.class);
            }

            if (manifest == null || manifest.animations() == null || manifest.animations().isEmpty()) {
                throw new LottieFileException("manifest has no animations");
            }

            // --- animations (a/ or animations/) ---
            List<Animation> parsedAnimations = new ArrayList<>();
            for (var manifestAnimation : manifest.animations()) {
                String animationId = manifestAnimation == null ? null : manifestAnimation.id();
                if (animationId == null || animationId.isBlank()) {
                    throw new LottieFileException("manifest contains animation with missing id");
                }

                ZipEntry animationEntry = resolveAnimationEntry(zipFile, animationId);
                if (animationEntry == null) {
                    throw new LottieFileException("missing animation JSON for id '" + animationId + "'");
                }

                parsedAnimations.add(parseJsonString(zipFile.getInputStream(animationEntry)));
            }

            // --- themes (t/) ---
            Map<String, byte[]> themes = loadDirectoryEntries(zipFile, DIR_THEMES, true);

            // --- state machines (s/) ---
            Map<String, byte[]> stateMachines = loadDirectoryEntries(zipFile, DIR_STATE_MACHINES, true);

            // --- image assets (i/) ---
            Map<String, byte[]> imageAssets = loadDirectoryEntries(zipFile, DIR_IMAGES, false);

            // --- font assets (f/) ---
            Map<String, byte[]> fontAssets = loadDirectoryEntries(zipFile, DIR_FONTS, false);

            return new DotLottie(manifest, parsedAnimations, themes, stateMachines, imageAssets, fontAssets);
        } catch (ZipException e) {
            throw new LottieFileException("zip error: " + e.getMessage());
        } catch (IOException e) {
            throw new LottieFileException("I/O error: " + e.getMessage());
        }
    }

    /**
     * Reads all entries under {@code prefix} from the zip into a map.
     * For JSON-only directories ({@code stripExtension=true}) the key is the base filename
     * without the {@code .json} suffix (i.e. the asset id). For binary directories the key
     * is the plain filename including its extension.
     *
     * @param zipFile         opened archive
     * @param prefix          directory prefix including trailing slash (e.g. {@code "t/"})
     * @param stripExtension  when {@code true}, strip the {@code .json} suffix from the key
     * @return map of id/filename → raw bytes; empty when the directory is absent
     */
    private static Map<String, byte[]> loadDirectoryEntries(ZipFile zipFile, String prefix,
                                                            boolean stripExtension) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<>();
        var entries = zipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (entry.isDirectory() || !name.startsWith(prefix)) {
                continue;
            }
            String filename = name.substring(prefix.length());
            if (filename.isEmpty()) {
                continue;
            }
            String key = (stripExtension && filename.endsWith(JSON_EXTENSION))
                    ? filename.substring(0, filename.length() - JSON_EXTENSION.length())
                    : filename;
            try (InputStream in = zipFile.getInputStream(entry)) {
                result.put(key, in.readAllBytes());
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Resolves the zip entry for an animation id using supported dotLottie folder layouts.
     *
     * @param zipFile     opened dotLottie archive
     * @param animationId manifest animation id
     * @return matching zip entry, or {@code null} when no supported path exists
     */
    private static ZipEntry resolveAnimationEntry(ZipFile zipFile, String animationId) {
        ZipEntry animationEntry = zipFile.getEntry(DIR_ANIMATIONS + animationId + JSON_EXTENSION);
        if (animationEntry == null) {
            animationEntry = zipFile.getEntry(DIR_ANIMATIONS_LEGACY + animationId + JSON_EXTENSION);
        }
        return animationEntry;
    }
}
