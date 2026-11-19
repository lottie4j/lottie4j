package com.lottie4j.fxplayer.util;

import com.lottie4j.core.definition.EffectType;
import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.helper.LayerHierarchy;
import com.lottie4j.core.helper.ModelWalker;
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
        Tally tally = new Tally();
        Map<String, Asset> compositions = compositions(animation);

        inspect(animation.layers(), compositions, tally);
        for (Asset composition : compositions.values()) {
            inspect(composition.layers(), compositions, tally);
            if (containsItself(composition, compositions)) {
                tally.add(Feature.SELF_CONTAINING_PRECOMPOSITION, null);
            }
        }
        return tally.findings();
    }

    /** The precompositions of an animation by id. */
    private static Map<String, Asset> compositions(Animation animation) {
        Map<String, Asset> compositions = new HashMap<>();
        if (animation.assets() != null) {
            for (Asset asset : animation.assets()) {
                if (asset != null && asset.id() != null && asset.layers() != null) {
                    compositions.putIfAbsent(asset.id(), asset);
                }
            }
        }
        return compositions;
    }

    /** Counts the uses of each feature and remembers which layers they are in. */
    private static final class Tally {

        private final Map<Feature, Integer> counts = new EnumMap<>(Feature.class);
        private final Map<Feature, Set<String>> names = new EnumMap<>(Feature.class);

        /**
         * @param layer the layer that uses the feature; {@code null} if it is not one layer's doing
         */
        void add(Feature feature, Layer layer) {
            counts.merge(feature, 1, Integer::sum);
            Set<String> list = names.computeIfAbsent(feature, key -> new LinkedHashSet<>());
            if (layer != null && list.size() < MAX_NAMES) {
                list.add(layer.name() != null ? layer.name() : "(unnamed)");
            }
        }

        void addIf(boolean used, Feature feature, Layer layer) {
            if (used) {
                add(feature, layer);
            }
        }

        List<Finding> findings() {
            List<Finding> findings = new ArrayList<>();
            for (Map.Entry<Feature, Integer> entry : counts.entrySet()) {
                findings.add(new Finding(entry.getKey(), entry.getValue(), List.copyOf(names.get(entry.getKey()))));
            }
            return findings;
        }
    }

    private static void inspect(List<Layer> layers, Map<String, Asset> compositions, Tally tally) {
        if (layers == null) {
            return;
        }
        Map<Integer, Layer> byIndex = LayerHierarchy.byIndex(layers);
        for (Layer layer : layers) {
            if (layer != null) {
                inspect(layer, byIndex, compositions, tally);
            }
        }
    }

    private static void inspect(Layer layer, Map<Integer, Layer> byIndex, Map<String, Asset> compositions,
                                Tally tally) {
        LayerType type = layer.layerType();
        tally.addIf(type == null || !RENDERED_LAYER_TYPES.contains(type), Feature.LAYER_TYPE, layer);
        tally.addIf(type == LayerType.PRECOMPOSITION && !compositions.containsKey(layer.referenceId()),
                Feature.MISSING_PRECOMPOSITION, layer);
        tally.addIf(layer.timeRemapping() != null, Feature.TIME_REMAPPING, layer);
        tally.addIf(layer.autoRotate() != null && layer.autoRotate() != 0, Feature.AUTO_ORIENT, layer);
        tally.addIf(hasCircularParents(layer, byIndex), Feature.CIRCULAR_PARENT, layer);
        if (layer.styles() != null) {
            layer.styles().forEach(style -> tally.add(Feature.LAYER_STYLE, layer));
        }
        if (layer.effects() != null) {
            layer.effects().forEach(effect -> tally.addIf(isUndrawnEffect(effect), Feature.EFFECT, layer));
        }
        ModelWalker.visit(layer, part -> inspectPart(part, layer, tally));
    }

    private static boolean hasCircularParents(Layer layer, Map<Integer, Layer> byIndex) {
        return layer.indexParent() != null && byIndex.containsKey(layer.indexParent())
                && LayerHierarchy.parentChain(layer, byIndex).isEmpty();
    }

    /** An effect that is switched on and is not the one kind the player draws. */
    private static boolean isUndrawnEffect(Effect effect) {
        boolean enabled = effect != null && (effect.enabled() == null || effect.enabled() != 0);
        return enabled && effect.type() != EffectType.GAUSSIAN_BLUR;
    }

    /** Looks at one record of the layer: a property, a transform, a shape... */
    private static void inspectPart(Object value, Layer layer, Tally tally) {
        tally.addIf(value instanceof Animated animated && animated.hasExpression(), Feature.EXPRESSION, layer);
        tally.addIf(isSkewed(value), Feature.SKEW, layer);
        tally.addIf(isUnappliedModifier(value), Feature.SHAPE_MODIFIER, layer);
        tally.addIf(value instanceof UnknownShape, Feature.UNKNOWN_SHAPE, layer);
    }

    private static boolean isSkewed(Object value) {
        if (value instanceof com.lottie4j.core.model.transform.Transform transform) {
            return skewed(transform.skew());
        }
        return value instanceof com.lottie4j.core.model.shape.grouping.Transform transform && skewed(transform.skew());
    }

    private static boolean isUnappliedModifier(Object value) {
        return value instanceof Repeater || value instanceof RoundedCorners || value instanceof OffsetPath
                || value instanceof Pucker || value instanceof Twist || value instanceof ZigZag;
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
