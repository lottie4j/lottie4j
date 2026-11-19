package com.lottie4j.core.file;

import tools.jackson.databind.ObjectMapper;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.asset.Asset;
import com.lottie4j.core.model.layer.Layer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lottie4j.core.exception.LottieFileException;
import com.lottie4j.core.model.dot.DotLottie;
import com.lottie4j.core.model.dot.Manifest;
import com.lottie4j.core.model.dot.ManifestAnimation;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.ArrayList;
import java.util.List;


/**
 * Utility class for exporting Lottie animations and individual layers to JSON files.
 * Handles an asset collection for precomposition layers to ensure self-contained exports.
 */
public class LottieFileSaver {

    private static final Logger logger = LoggerFactory.getLogger(LottieFileSaver.class);
    private static final ObjectMapper mapper = ObjectMapperFactory.getInstance();

    /**
     * Prevents instantiation of this utility class.
     */
    private static final String IO_ERROR = "I/O error: ";
    private static final String JSON_EXTENSION = ".json";
    private static final String VERSION_2 = "2";
    private static final String GENERATOR = "lottie4j";

    private LottieFileSaver() {
        // Hide constructor
    }

    /**
     * Export a single layer as a standalone JSON animation file.
     * Automatically collects all required assets including nested precompositions.
     *
     * @param animation  source animation containing the layer
     * @param layer      layer to export
     * @param layerIndex index of the layer (used for naming if layer has no name)
     * @param outputFile destination file for the exported JSON
     * @throws RuntimeException if export fails
     */
    public static void saveLayer(Animation animation, Layer layer, int layerIndex, File outputFile) {
        try {
            // Collect required assets (recursively)
            var requiredAssets = collectRequiredAssets(layer, animation);

            // Create a new Animation with only this layer
            Animation export = new Animation(
                    animation.version() != null ? animation.version() : "5.5.0",
                    layer.name() != null ? layer.name() : ("Layer " + layerIndex),
                    animation.matchName(),
                    animation.has3dLayers() != null ? animation.has3dLayers() : 0,
                    animation.framesPerSecond() != null ? animation.framesPerSecond() : 60,
                    animation.inPoint() != null ? animation.inPoint() : 0,
                    animation.outPoint() != null ? animation.outPoint() : 100,
                    animation.width() != null ? animation.width() : 1280,
                    animation.height() != null ? animation.height() : 720,
                    requiredAssets,
                    List.of(layer),
                    null,
                    animation.props()
            );

            // Write to file using Jackson - Animation record handles serialization
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, export);
            logger.info("Successfully exported layer to: {}", outputFile.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to export layer to JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Export failed", e);
        }
    }

    /**
     * Collect all assets required by a layer (recursively for precompositions).
     *
     * @param layer     layer whose assets should be collected
     * @param animation source animation containing asset definitions
     * @return list of required assets without duplicates
     */
    private static List<Asset> collectRequiredAssets(Layer layer, Animation animation) {
        var result = new java.util.LinkedHashSet<Asset>(); // Use LinkedHashSet to avoid duplicates
        collectRequiredAssetsRecursive(layer, animation, result);
        return new ArrayList<>(result);
    }

    /**
     * Recursive helper to collect all assets referenced by a layer and its children.
     *
     * @param layer     layer to analyze
     * @param animation source animation containing asset definitions
     * @param collected set accumulating unique assets
     */
    private static void collectRequiredAssetsRecursive(Layer layer, Animation animation, java.util.Set<Asset> collected) {
        if (layer.referenceId() != null && animation.assets() != null) {
            // This layer references an asset (precomposition)
            String refId = layer.referenceId();

            for (Asset asset : animation.assets()) {
                if (asset.id() != null && asset.id().equals(refId)) {
                    // Add this asset if not already collected
                    if (!collected.contains(asset)) {
                        collected.add(asset);

                        // Recursively collect assets from nested layers
                        if (asset.layers() != null) {
                            for (Layer nestedLayer : asset.layers()) {
                                collectRequiredAssetsRecursive(nestedLayer, animation, collected);
                            }
                        }
                    }
                    break;
                }
            }
        }
    }

    /**
     * Writes an animation as Lottie JSON.
     *
     * @param animation the animation
     * @param file      the file to write, usually with the extension {@code .json}
     * @throws LottieFileException if the animation cannot be written
     */
    public static void save(Animation animation, File file) throws LottieFileException {
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            save(animation, out);
        } catch (IOException e) {
            throw new LottieFileException(IO_ERROR + e.getMessage());
        }
    }

    /**
     * Writes an animation as Lottie JSON. The stream is not closed.
     *
     * @param animation the animation
     * @param out       the stream to write to
     * @throws LottieFileException if the animation cannot be written
     */
    public static void save(Animation animation, OutputStream out) throws LottieFileException {
        try {
            out.write(json(animation));
        } catch (IOException e) {
            throw new LottieFileException(IO_ERROR + e.getMessage());
        }
    }

    /**
     * Writes one animation as a dotLottie archive of version 2: a manifest that names the
     * animation and the animation itself. Images and sounds stay where the animation has them,
     * which for a file that is to be complete means embedded as data URIs.
     *
     * @param animation the animation
     * @param id        the id of the animation in the archive; it becomes a file name
     * @param out       the stream to write to; it is not closed
     * @throws LottieFileException if the archive cannot be written
     */
    public static void saveDotLottie(Animation animation, String id, OutputStream out) throws LottieFileException {
        Manifest manifest = new Manifest(VERSION_2, GENERATOR, null,
                List.of(new ManifestAnimation(id, null, null, null, null, null, null, null, null)), null, null,
                null);
        saveDotLottie(new DotLottie(manifest, List.of(animation), null, null, null, null), out);
    }

    /**
     * Writes a dotLottie archive.
     *
     * @param dotLottie the content of the archive
     * @param file      the file to write, usually with the extension {@code .lottie}
     * @throws LottieFileException if the archive cannot be written
     */
    public static void saveDotLottie(DotLottie dotLottie, File file) throws LottieFileException {
        try (OutputStream out = Files.newOutputStream(file.toPath())) {
            saveDotLottie(dotLottie, out);
        } catch (IOException e) {
            throw new LottieFileException(IO_ERROR + e.getMessage());
        }
    }

    /**
     * Writes a dotLottie archive: {@code manifest.json}, one JSON file per animation the manifest
     * lists, and the themes, state machines, images and fonts. A manifest of version 1 gets the
     * directories of that version ({@code animations/}, {@code images/}), any other the short
     * ones of version 2 ({@code a/}, {@code i/}).
     *
     * @param dotLottie the content of the archive; the manifest must list as many animations as there are
     * @param out       the stream to write to; it is not closed
     * @throws LottieFileException if the content is incomplete or the archive cannot be written
     */
    public static void saveDotLottie(DotLottie dotLottie, OutputStream out) throws LottieFileException {
        Manifest manifest = dotLottie.manifest();
        List<Animation> animations = dotLottie.animations();
        if (manifest == null || manifest.animations() == null || manifest.animations().isEmpty()) {
            throw new LottieFileException("manifest has no animations");
        }
        if (animations == null || animations.size() != manifest.animations().size()) {
            throw new LottieFileException("manifest does not list one entry per animation");
        }
        boolean legacy = manifest.version() != null && manifest.version().startsWith("1");
        ZipOutputStream zip = new ZipOutputStream(out);
        try {
            entry(zip, "manifest.json", mapper.writeValueAsBytes(manifest));
            for (int i = 0; i < animations.size(); i++) {
                String id = manifest.animations().get(i) == null ? null : manifest.animations().get(i).id();
                if (id == null || id.isBlank() || id.contains("/") || id.contains("\\") || id.contains("..")) {
                    throw new LottieFileException("manifest contains animation with missing or unusable id");
                }
                entry(zip, (legacy ? "animations/" : "a/") + id + JSON_EXTENSION, json(animations.get(i)));
            }
            entries(zip, "t/", JSON_EXTENSION, dotLottie.themes());
            entries(zip, "s/", JSON_EXTENSION, dotLottie.stateMachines());
            entries(zip, legacy ? "images/" : "i/", "", dotLottie.imageAssets());
            entries(zip, "f/", "", dotLottie.fontAssets());
            zip.finish();
        } catch (IOException e) {
            throw new LottieFileException(IO_ERROR + e.getMessage());
        }
    }

    private static byte[] json(Animation animation) throws LottieFileException {
        if (animation == null) {
            throw new LottieFileException("no animation to write");
        }
        try {
            return mapper.writeValueAsBytes(animation);
        } catch (RuntimeException e) {
            throw new LottieFileException("JSON could not be written: " + e.getMessage());
        }
    }

    private static void entries(ZipOutputStream zip, String directory, String extension, Map<String, byte[]> files)
            throws IOException {
        if (files == null) {
            return;
        }
        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            entry(zip, directory + file.getKey() + extension, file.getValue());
        }
    }

    private static void entry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content);
        zip.closeEntry();
    }
}
