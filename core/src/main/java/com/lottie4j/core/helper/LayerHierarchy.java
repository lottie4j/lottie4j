package com.lottie4j.core.helper;

import com.lottie4j.core.model.layer.Layer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the references between the layers of one composition: a layer to its parent
 * ({@code parent}) and a layer to the layer it takes its track matte from ({@code tp}, or the
 * layer above it). The references are indices ({@code ind}) a file is free to get wrong, so
 * every lookup here ends, also on a chain that runs in a circle.
 */
public final class LayerHierarchy {

    private static final Logger logger = LoggerFactory.getLogger(LayerHierarchy.class);

    private LayerHierarchy() {
    }

    /**
     * Indexes the layers of a composition by their {@code ind}. Of two layers with the same index
     * the first one counts.
     *
     * @param layers the layers of one composition; may be {@code null}
     * @return the layers that have an index, by that index
     */
    public static Map<Integer, Layer> byIndex(List<Layer> layers) {
        Map<Integer, Layer> result = new HashMap<>();
        if (layers != null) {
            for (Layer layer : layers) {
                if (layer != null && layer.indexLayer() != null) {
                    result.putIfAbsent(layer.indexLayer(), layer);
                }
            }
        }
        return result;
    }

    /**
     * The ancestors of a layer, outermost first, which is the order their transforms apply in.
     * A chain that names a layer that does not exist ends there. A chain that runs in a circle
     * has no outermost layer to start from: the layer is treated as having no parent at all.
     *
     * @param layer   the layer
     * @param byIndex the layers of its composition, see {@link #byIndex(List)}
     * @return the ancestors, outermost first; empty if there are none
     */
    public static List<Layer> parentChain(Layer layer, Map<Integer, Layer> byIndex) {
        List<Layer> chain = new ArrayList<>();
        Set<Layer> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        seen.add(layer);
        Integer parentIndex = layer.indexParent();
        while (parentIndex != null) {
            Layer parent = byIndex.get(parentIndex);
            if (parent == null) {
                break;
            }
            if (!seen.add(parent)) {
                logger.warn("The parents of layer \"{}\" run in a circle; it is treated as having no parent",
                        layer.name());
                return List.of();
            }
            chain.add(parent);
            parentIndex = parent.indexParent();
        }
        Collections.reverse(chain);
        return chain;
    }

    /**
     * The layer a layer takes its track matte from. If the layer names one with {@code tp}, that
     * is the one. Otherwise it is the layer listed just before it, which is the one above it in
     * After Effects, provided that layer is marked as a matte ({@code td}).
     *
     * @param layers   the layers of the composition, in file order
     * @param position the position of the layer in that list
     * @param byIndex  the layers of the composition, see {@link #byIndex(List)}
     * @return the matte layer, or {@code null} if the layer has no track matte or its matte cannot be found
     */
    public static Layer matteSource(List<Layer> layers, int position, Map<Integer, Layer> byIndex) {
        Layer layer = layers.get(position);
        if (layer.matteMode() == null) {
            return null;
        }
        if (layer.tp() != null) {
            Layer named = byIndex.get(layer.tp());
            return named == layer ? null : named;
        }
        if (position > 0) {
            Layer above = layers.get(position - 1);
            if (above != null && above.matteTarget() != null && above.matteTarget() == 1) {
                return above;
            }
        }
        return null;
    }

    /**
     * The layers that serve as a track matte for another layer of the composition, by {@code tp}
     * or by position. They are drawn through the layer they are a matte for, not on their own.
     *
     * @param layers  the layers of the composition, in file order
     * @param byIndex the layers of the composition, see {@link #byIndex(List)}
     * @return the matte layers
     */
    public static Set<Layer> matteSources(List<Layer> layers, Map<Integer, Layer> byIndex) {
        Set<Layer> result = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < layers.size(); i++) {
            Layer source = matteSource(layers, i, byIndex);
            if (source != null) {
                result.add(source);
            }
        }
        return result;
    }
}
