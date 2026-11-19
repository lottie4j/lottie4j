package com.lottie4j.fxplayer.util;

import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.asset.Asset;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes the shapes that are switched off in a file ({@code "hd": true}) out of an animation.
 * <p>
 * A hidden shape draws nothing; a hidden fill, stroke or modifier applies to nothing. The
 * renderers look at the shapes of a group in many places, so the hidden ones are removed once,
 * before rendering, instead of being skipped in each of those places. Layers are left where they
 * are, hidden or not: other layers refer to them by position and by index.
 */
public final class HiddenShapes {

    private HiddenShapes() {
    }

    /**
     * Removes the hidden shapes of every layer, also inside groups and precompositions.
     *
     * @param animation the animation as loaded
     * @return the same animation if it has no hidden shapes, otherwise a copy without them
     */
    public static Animation strip(Animation animation) {
        if (animation == null) {
            return null;
        }
        List<Layer> layers = stripLayers(animation.layers());
        List<Asset> assets = stripAssets(animation.assets());
        return layers == animation.layers() && assets == animation.assets()
                ? animation : animation.withLayersAndAssets(layers, assets);
    }

    private static List<Asset> stripAssets(List<Asset> assets) {
        if (assets == null) {
            return assets;
        }
        List<Asset> result = assets;
        for (int i = 0; i < assets.size(); i++) {
            Asset asset = assets.get(i);
            List<Layer> layers = asset == null ? null : stripLayers(asset.layers());
            if (asset != null && layers != asset.layers()) {
                result = result == assets ? new ArrayList<>(assets) : result;
                result.set(i, asset.withLayers(layers));
            }
        }
        return result;
    }

    private static List<Layer> stripLayers(List<Layer> layers) {
        if (layers == null) {
            return layers;
        }
        List<Layer> result = layers;
        for (int i = 0; i < layers.size(); i++) {
            Layer layer = layers.get(i);
            if (layer == null || layer.shapes() == null) {
                continue;
            }
            List<BaseShape> shapes = stripShapes(layer.shapes());
            if (shapes != layer.shapes()) {
                result = result == layers ? new ArrayList<>(layers) : result;
                result.set(i, layer.withShapes(shapes));
            }
        }
        return result;
    }

    private static List<BaseShape> stripShapes(List<BaseShape> shapes) {
        List<BaseShape> result = new ArrayList<>(shapes.size());
        boolean changed = false;
        for (BaseShape shape : shapes) {
            if (shape == null || shape.isHidden()) {
                changed = true;
                continue;
            }
            if (shape instanceof Group group && group.shapes() != null) {
                List<BaseShape> inner = stripShapes(group.shapes());
                if (inner != group.shapes()) {
                    shape = group.withShapes(inner);
                    changed = true;
                }
            }
            result.add(shape);
        }
        return changed ? result : shapes;
    }
}
