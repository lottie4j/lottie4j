package com.lottie4j.core.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lottie4j.core.helper.ObjectMapperFactory;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Asset;
import com.lottie4j.core.model.Layer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class LottieFileSaver {

    private static final Logger logger = Logger.getLogger(LottieFileSaver.class.getName());
    private static final ObjectMapper mapper = ObjectMapperFactory.getInstance();

    private LottieFileSaver() {
        // Hide constructor
    }

    /**
     * Export a single layer as a standalone JSON animation file
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
                    java.util.List.of(layer)
            );

            // Write to file using Jackson - Animation record handles serialization
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, export);
            logger.info("Successfully exported layer to: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            logger.severe("Failed to export layer to JSON: " + e.getMessage());
            throw new RuntimeException("Export failed", e);
        }
    }

    /**
     * Collect all assets required by a layer (recursively for precompositions)
     */
    private static List<Asset> collectRequiredAssets(Layer layer, Animation animation) {
        var result = new java.util.LinkedHashSet<Asset>(); // Use LinkedHashSet to avoid duplicates
        collectRequiredAssetsRecursive(layer, animation, result);
        return new ArrayList<>(result);
    }

    /**
     * Recursive helper to collect all assets
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
}
