package com.lottie4j.core.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.dot.DotLottie;
import com.lottie4j.core.model.dot.Manifest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
     * Loads a dotLottie archive, parses {@code manifest.json}, and resolves all animations listed in the manifest.
     *
     * @param file `.lottie` zip archive
     * @return populated dotLottie object containing the parsed manifest and animations
     * @throws LottieFileException if the archive is invalid, required entries are missing,
     *                             or manifest/animation content cannot be parsed
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

            return new DotLottie(manifest, parsedAnimations);
        } catch (ZipException e) {
            throw new LottieFileException("zip error: " + e.getMessage());
        } catch (IOException e) {
            throw new LottieFileException("I/O error: " + e.getMessage());
        }
    }

    /**
     * Resolves the zip entry for an animation id using supported dotLottie folder layouts.
     *
     * @param zipFile     opened dotLottie archive
     * @param animationId manifest animation id
     * @return matching zip entry, or {@code null} when no supported path exists
     */
    private static ZipEntry resolveAnimationEntry(ZipFile zipFile, String animationId) {
        ZipEntry animationEntry = zipFile.getEntry("a/" + animationId + JSON_EXTENSION);
        if (animationEntry == null) {
            animationEntry = zipFile.getEntry("animations/" + animationId + JSON_EXTENSION);
        }
        return animationEntry;
    }
}
