package com.lottie4j.fxplayer.util;

import com.lottie4j.core.definition.EffectType;
import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.helper.LayerHierarchy;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.asset.Asset;
import com.lottie4j.core.model.effect.Effect;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.core.model.shape.UnknownShape;
import com.lottie4j.core.model.shape.modifier.OffsetPath;
import com.lottie4j.core.model.shape.modifier.Pucker;
import com.lottie4j.core.model.shape.modifier.Repeater;
import com.lottie4j.core.model.shape.modifier.RoundedCorners;
import com.lottie4j.core.model.shape.modifier.Twist;
import com.lottie4j.core.model.shape.modifier.ZigZag;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tells what a Lottie file uses that this player does not draw, or does not draw as the file
 * means it. A file that looks wrong usually looks wrong for one of these reasons, and this is the
 * quickest way to find out which.
 * <p>
 * The list is of what the player is known not to do. A file without findings uses nothing from
 * this list; that is not a promise that every frame matches other players.
 */
public final class UnsupportedFeatures {

    /** How many layer names a finding lists at most. */
    private static final int MAX_NAMES = 10;

    private static final Set<LayerType> RENDERED_LAYER_TYPES = Set.of(LayerType.PRECOMPOSITION,
            LayerType.SOLD_COLOR, LayerType.IMAGE, LayerType.NULL, LayerType.SHAPE, LayerType.TEXT);

    private UnsupportedFeatures() {
    }

    /** Something a file can use that the player does not draw as meant. */
    public enum Feature {
        /** A property with an expression. */
        EXPRESSION("Expressions are not evaluated; the property has the value it has without them"),
        /** A precomposition with time remapping. */
        TIME_REMAPPING("Time remapping is ignored; the precomposition plays at its own time"),
        /** A layer that turns along its motion path. */
        AUTO_ORIENT("Auto-orient is ignored; the layer keeps its own rotation"),
        /** A transform with a skew. */
        SKEW("Skew is ignored"),
        /** A layer style such as a drop shadow or a stroke. */
        LAYER_STYLE("Layer styles are not drawn"),
        /** A layer effect the player has no implementation for. */
        EFFECT("Layer effects other than Gaussian blur are not drawn"),
        /** A layer of a type that has no content the player draws. */
        LAYER_TYPE("Layers of this type are not drawn (audio, video, camera, light, or a type this library does not know)"),
        /** A shape modifier that is not applied. */
        SHAPE_MODIFIER("Repeater, rounded corners, offset path, pucker, twist and zig-zag are not applied"),
        /** A shape of a type this library does not know. */
        UNKNOWN_SHAPE("Shapes of a type this library does not know are not drawn"),
        /** A precomposition layer whose composition is not in the file. */
        MISSING_PRECOMPOSITION("The precomposition the layer refers to is not in the file; nothing is drawn"),
        /** A precomposition that contains itself. */
        SELF_CONTAINING_PRECOMPOSITION("A precomposition contains itself; the inner one is not drawn"),
        /** Layers whose parents refer to each other. */
        CIRCULAR_PARENT("The parents of the layer run in a circle; it is drawn as if it had no parent");

        private final String description;

        Feature(String description) {
            this.description = description;
        }

        /**
         * What the player does instead.
         *
         * @return a sentence for a person to read
         */
        public String description() {
            return description;
        }
    }

    /**
     * One feature a file uses.
     *
     * @param feature the feature
     * @param count   how often the file uses it
     * @param layers  the names of the layers that use it, in file order, without repeats and at
     *                most ten of them
     */
    public record Finding(Feature feature, int count, List<String> layers) {

        /**
         * The finding as one line of text.
         *
         * @return the description, the count and the layers
         */
        public String describe() {
            return feature.description() + " (" + count + "x" + (layers.isEmpty() ? "" : ", in " + String.join(", ", layers))
                    + ")";
        }
    }

    /**
     * Looks through an animation, including its precompositions.
     *
     * @param animation the animation
     * @return one finding per feature the animation uses, in the order of {@link Feature}; empty
     *         if it uses none of them
     */
    public static List<Finding> find(Animation animation) {
        Map<Feature, Integer> counts = new EnumMap<>(Feature.class);
        Map<Feature, Set<String>> names = new EnumMap<>(Feature.class);
        Map<String, Asset> compositions = new HashMap<>();
        if (animation.assets() != null) {
            for (Asset asset : animation.assets()) {
                if (asset != null && asset.id() != null && asset.layers() != null) {
                    compositions.putIfAbsent(asset.id(), asset);
                }
            }
        }
        Collector collector = (feature, layer) -> {
            counts.merge(feature, 1, Integer::sum);
            Set<String> list = names.computeIfAbsent(feature, key -> new LinkedHashSet<>());
            if (list.size() < MAX_NAMES) {
                list.add(layer.name() != null ? layer.name() : "(unnamed)");
            }
        };

        inspect(animation.layers(), compositions, collector);
        for (Asset composition : compositions.values()) {
            inspect(composition.layers(), compositions, collector);
            if (containsItself(composition, compositions)) {
                counts.merge(Feature.SELF_CONTAINING_PRECOMPOSITION, 1, Integer::sum);
                names.computeIfAbsent(Feature.SELF_CONTAINING_PRECOMPOSITION, key -> new LinkedHashSet<>());
            }
        }

        List<Finding> findings = new ArrayList<>();
        for (Feature feature : Feature.values()) {
            if (counts.containsKey(feature)) {
                findings.add(new Finding(feature, counts.get(feature), List.copyOf(names.get(feature))));
            }
        }
        return findings;
    }

    private interface Collector {
        void add(Feature feature, Layer layer);
    }

    private static void inspect(List<Layer> layers, Map<String, Asset> compositions, Collector collector) {
        if (layers == null) {
            return;
        }
        Map<Integer, Layer> byIndex = LayerHierarchy.byIndex(layers);
        for (Layer layer : layers) {
            if (layer == null) {
                continue;
            }
            if (layer.layerType() == null || !RENDERED_LAYER_TYPES.contains(layer.layerType())) {
                collector.add(Feature.LAYER_TYPE, layer);
            }
            if (layer.layerType() == LayerType.PRECOMPOSITION && !compositions.containsKey(layer.referenceId())) {
                collector.add(Feature.MISSING_PRECOMPOSITION, layer);
            }
            if (layer.timeRemapping() != null) {
                collector.add(Feature.TIME_REMAPPING, layer);
            }
            if (layer.autoRotate() != null && layer.autoRotate() != 0) {
                collector.add(Feature.AUTO_ORIENT, layer);
            }
            if (layer.styles() != null) {
                layer.styles().forEach(style -> collector.add(Feature.LAYER_STYLE, layer));
            }
            if (layer.effects() != null) {
                for (Effect effect : layer.effects()) {
                    boolean enabled = effect != null && (effect.enabled() == null || effect.enabled() != 0);
                    if (enabled && effect.type() != EffectType.GAUSSIAN_BLUR) {
                        collector.add(Feature.EFFECT, layer);
                    }
                }
            }
            if (layer.indexParent() != null && byIndex.containsKey(layer.indexParent())
                    && LayerHierarchy.parentChain(layer, byIndex).isEmpty()) {
                collector.add(Feature.CIRCULAR_PARENT, layer);
            }
            walk(layer, layer, collector, 0);
        }
    }

    /**
     * Goes through everything a layer is made of. The model is records and lists of records all
     * the way down, so the components of each record are what there is to look at.
     */
    private static void walk(Object value, Layer layer, Collector collector, int depth) {
        if (value == null || depth > 200) {
            return;
        }
        if (value instanceof List<?> list) {
            for (Object element : list) {
                walk(element, layer, collector, depth + 1);
            }
            return;
        }
        if (!value.getClass().isRecord()) {
            return;
        }
        if (value instanceof Animated animated && animated.hasExpression()) {
            collector.add(Feature.EXPRESSION, layer);
        }
        if (value instanceof com.lottie4j.core.model.transform.Transform transform && skewed(transform.skew())
                || value instanceof com.lottie4j.core.model.shape.grouping.Transform shapeTransform
                && skewed(shapeTransform.skew())) {
            collector.add(Feature.SKEW, layer);
        }
        if (value instanceof Repeater || value instanceof RoundedCorners || value instanceof OffsetPath
                || value instanceof Pucker || value instanceof Twist || value instanceof ZigZag) {
            collector.add(Feature.SHAPE_MODIFIER, layer);
        }
        if (value instanceof UnknownShape) {
            collector.add(Feature.UNKNOWN_SHAPE, layer);
        }
        for (RecordComponent component : value.getClass().getRecordComponents()) {
            try {
                walk(component.getAccessor().invoke(value), layer, collector, depth + 1);
            } catch (ReflectiveOperationException | RuntimeException e) {
                // a component that cannot be read has nothing to report
            }
        }
    }

    private static boolean skewed(Animated skew) {
        if (skew == null || skew.keyframes() == null || skew.keyframes().isEmpty()) {
            return false;
        }
        boolean animated = skew.animated() != null && skew.animated() != 0;
        return animated || skew.getValue(0) != 0;
    }

    /** Whether a composition can be reached from itself through the precomposition layers it holds. */
    private static boolean containsItself(Asset composition, Map<String, Asset> compositions) {
        Set<String> seen = new HashSet<>();
        List<Asset> open = new ArrayList<>(List.of(composition));
        while (!open.isEmpty()) {
            Asset current = open.remove(open.size() - 1);
            for (Layer layer : current.layers()) {
                if (layer == null || layer.layerType() != LayerType.PRECOMPOSITION || layer.referenceId() == null) {
                    continue;
                }
                if (layer.referenceId().equals(composition.id())) {
                    return true;
                }
                Asset next = compositions.get(layer.referenceId());
                if (next != null && seen.add(layer.referenceId())) {
                    open.add(next);
                }
            }
        }
        return false;
    }
}
