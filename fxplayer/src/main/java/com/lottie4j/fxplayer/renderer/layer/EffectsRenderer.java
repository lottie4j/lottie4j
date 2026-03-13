package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.definition.EffectType;
import com.lottie4j.core.model.animation.Animated;
import com.lottie4j.core.model.bezier.AnimatedBezier;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.fxplayer.util.OffscreenRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.lang.reflect.RecordComponent;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies layer-level effects that need JavaFX-side post-processing after the layer content has been drawn.
 *
 * <p>The current implementation focuses on Lottie's Gaussian Blur effect. It translates effect values from
 * Lottie space into JavaFX-compatible blur radii, optionally renders through an off-screen buffer when the
 * blur must be cropped to known layer bounds, and can cache expensive blurred rasters for frame-invariant
 * layers.
 *
 * <p>Instances are reusable across frames. The internal static-blur cache is keyed by layer identity,
 * raster size, and sampled blur radius.
 */
public class EffectsRenderer {

    private static final Logger logger = LoggerFactory.getLogger(EffectsRenderer.class);
    private static final double STATIC_BLUR_CACHE_MIN_RADIUS = 20.0;
    private static final long STATIC_BLUR_CACHE_MAX_PIXEL_COUNT = 8_000_000L;

    private final Map<StaticLayerBlurCacheKey, WritableImage> staticLayerBlurCache = new ConcurrentHashMap<>();

    /**
     * Creates a new effects renderer.
     *
     * <p>The renderer keeps a per-instance cache for static blurred layer rasters, so callers typically reuse
     * a single instance for the lifetime of a player or renderer pipeline.
     */
    public EffectsRenderer() {
        // Constructor for EffectsRenderer
    }

    /**
     * Extracts the effective Gaussian blur radius for a layer at the supplied frame.
     *
     * <p>The method searches the layer effect list for the first enabled
     * {@link com.lottie4j.core.definition.EffectType#GAUSSIAN_BLUR} entry and then samples its
     * {@code Blurriness} property. Because JavaFX blur effects saturate at smaller radii than Lottie-web,
     * the sampled value is mapped through a heuristic scale curve and clamped to JavaFX's practical maximum.
     *
     * @param layer layer whose effects should be inspected; must not be {@code null}
     * @param frame animation frame to sample from the effect value curve
     * @return a JavaFX-ready blur radius in the range {@code [0, 63]}; returns {@code 0.0} when the layer has
     * no enabled Gaussian blur effect or no usable blur value
     */
    public double getGaussianBlurRadius(Layer layer, double frame) {
        // Default to no blur
        double blurRadius = 0.0;

        // Check if the layer has effects
        if (layer.effects() == null) {
            logger.debug("Layer {} has NO effects", layer.name());
            return blurRadius;
        }

        logger.debug("Layer {} has {} effects", layer.name(), layer.effects().size());
        // Log track matte and mask info for blur layers
        logger.debug("  Layer matteMode: {}, hasMask: {}", layer.matteMode(), layer.masks() != null && !layer.masks().isEmpty());

        for (var effect : layer.effects()) {
            logger.debug("  Effect type: {}, enabled: {}", effect.type(), effect.enabled());
            // Look for Gaussian Blur effect (type 14)
            if (effect.type() != EffectType.GAUSSIAN_BLUR) {
                continue;
            }

            // Skip if effect is disabled
            if (effect.enabled() != null && effect.enabled() == 0) {
                continue;
            }

            // Look for "Blurriness" property in the effect values
            if (effect.values() != null) {
                logger.debug("  Effect has {} values", effect.values().size());
                for (var effectValue : effect.values()) {
                    logger.debug("    Value name: {}", effectValue != null ? effectValue.name() : "null");
                    if (effectValue != null && "Blurriness".equals(effectValue.name())) {
                        if (effectValue.value() != null) {
                            double rawBlur = effectValue.value().getValue(0, frame);

                            // The key insight: Lottie-web's blur values of 700-800 create VERY diffuse blur
                            // that makes shapes blend together into gradient-like appearances.
                            // JavaFX blur is fundamentally limited to 63px radius.
                            // Strategy: Use maximum blur (63) for all extreme values and rely on opacity
                            // to create the gradient effect when multiple blurred shapes overlap.

                            if (rawBlur > 400) {
                                // Extreme blur (400+) - always use maximum
                                blurRadius = 63.0;
                            } else if (rawBlur > 200) {
                                // Very high blur - scale to near maximum
                                blurRadius = 50 + ((rawBlur - 200) / 200.0) * 13.0;
                            } else if (rawBlur > 100) {
                                // High blur
                                blurRadius = 35 + ((rawBlur - 100) / 100.0) * 15.0;
                            } else if (rawBlur > 50) {
                                // Medium-high blur
                                blurRadius = 20 + ((rawBlur - 50) / 50.0) * 15.0;
                            } else if (rawBlur > 20) {
                                // Medium blur
                                blurRadius = 10 + ((rawBlur - 20) / 30.0) * 10.0;
                            } else {
                                // Light blur
                                blurRadius = rawBlur / 2.0;
                            }

                            // Ensure we never exceed JavaFX's limit
                            blurRadius = Math.min(63, blurRadius);

                            logger.debug("Gaussian Blur FOUND: raw={} scaled={} for layer {} at frame {}", rawBlur, blurRadius, layer.name(), frame);
                        }
                        break;
                    }
                }
            }
            break;
        }

        return blurRadius;
    }

    /**
     * Renders a layer with blur applied and no explicit clipping bounds.
     *
     * <p>This is a convenience overload that delegates to
     * {@link #renderLayerWithGaussianBlur(GraphicsContext, Layer, double, double, double, double, LayerRenderer)}
     * with bounds disabled. The supplied callback is expected to render the layer in the current local
     * coordinate system.
     *
     * @param gc            destination graphics context whose current effect state will be temporarily replaced
     * @param layer         layer being rendered, used for logging only
     * @param frame         animation frame to render
     * @param blurRadius    JavaFX blur radius to apply before invoking {@code layerRenderer}
     * @param layerRenderer callback that draws the layer content into {@code gc}
     */
    public void renderLayerWithGaussianBlur(GraphicsContext gc, Layer layer, double frame, double blurRadius, LayerRenderer layerRenderer) {
        renderLayerWithGaussianBlur(gc, layer, frame, blurRadius, -1, -1, 1.0, layerRenderer);
    }

    /**
     * Renders a layer with blur applied, optionally cropping the blurred result to known local bounds.
     *
     * <p>When both bounds are positive, the layer is first rendered into an expanded off-screen image so the
     * blur kernel has room to spread, then only the central region matching the original layer bounds is drawn
     * back to the destination context. When either bound is non-positive, the blur effect is applied directly
     * to the live {@link GraphicsContext}.
     *
     * @param gc            destination graphics context; its effect stack is restored before the method returns
     * @param layer         layer being rendered, used for logging and diagnostics
     * @param frame         animation frame to render
     * @param blurRadius    JavaFX blur radius to apply
     * @param boundsWidth   clipping width in the layer's local coordinate space; values less than or equal to
     *                      {@code 0} disable bounded rendering
     * @param boundsHeight  clipping height in the layer's local coordinate space; values less than or equal to
     *                      {@code 0} disable bounded rendering
     * @param layerRenderer callback that renders the unblurred layer content in local coordinates with origin
     *                      at the top-left of the target bounds
     */
    public void renderLayerWithGaussianBlur(GraphicsContext gc,
                                            Layer layer,
                                            double frame,
                                            double blurRadius,
                                            double boundsWidth,
                                            double boundsHeight,
                                            LayerRenderer layerRenderer) {
        renderLayerWithGaussianBlur(gc, layer, frame, blurRadius, boundsWidth, boundsHeight, 1.0, layerRenderer);
    }

    /**
     * Renders a layer with Gaussian blur and optional bounds clipping at a configurable off-screen resolution scale.
     *
     * @param gc                    destination graphics context
     * @param layer                 layer to render
     * @param frame                 animation frame
     * @param blurRadius            blur radius in composition-space pixels
     * @param boundsWidth           clipping width in composition-space coordinates
     * @param boundsHeight          clipping height in composition-space coordinates
     * @param renderResolutionScale off-screen raster scale factor in range {@code (0, 1]}
     * @param layerRenderer         callback to render layer content
     */
    public void renderLayerWithGaussianBlur(GraphicsContext gc,
                                            Layer layer,
                                            double frame,
                                            double blurRadius,
                                            double boundsWidth,
                                            double boundsHeight,
                                            double renderResolutionScale,
                                            LayerRenderer layerRenderer) {
        double effectiveScale = Math.clamp(renderResolutionScale, 0.1, 1.0);
        logger.debug("renderLayerWithGaussianBlur called for layer {}: blurRadius={}, boundsWidth={}, boundsHeight={}, renderResolutionScale={}",
                layer.name(), blurRadius, boundsWidth, boundsHeight, effectiveScale);

        if (boundsWidth > 0 && boundsHeight > 0) {
            renderWithBoundedBlur(gc, layer, frame, blurRadius, boundsWidth, boundsHeight, effectiveScale, layerRenderer);
            return;
        }

        gc.save();
        double scaledBlur = blurRadius * effectiveScale;
        applyBlurEffect(gc, scaledBlur);
        layerRenderer.render(gc, layer, frame);
        gc.setEffect(null);
        gc.restore();
        logger.debug("Applied direct blur (no bounds) for layer {}: radius={} (scaled={})", layer.name(), blurRadius, scaledBlur);
    }

    /**
     * Renders into an off-screen buffer with padding, blurs the result, and draws back only the cropped center.
     *
     * <p>The off-screen image is expanded by roughly two blur radii on each side so the blur can bleed beyond
     * the original geometry. Only the source rectangle corresponding to the original bounds is copied back to
     * the destination context, ensuring consistent clipping even when direct JavaFX clipping and effects do not
     * interact as expected.
     *
     * @param gc            destination graphics context
     * @param layer         layer being rendered
     * @param frame         animation frame to render
     * @param blurRadius    blur radius used for padding and JavaFX effect selection
     * @param boundsWidth   unclipped local width of the layer content
     * @param boundsHeight  unclipped local height of the layer content
     * @param layerRenderer callback that renders the layer into the padded off-screen context
     */
    private void renderWithBoundedBlur(GraphicsContext gc,
                                       Layer layer,
                                       double frame,
                                       double blurRadius,
                                       double boundsWidth,
                                       double boundsHeight,
                                       double renderResolutionScale,
                                       LayerRenderer layerRenderer) {
        double effectiveScale = Math.clamp(renderResolutionScale, 0.1, 1.0);
        double scaledBlurRadius = blurRadius * effectiveScale;

        double padding = Math.ceil(scaledBlurRadius * 2);
        double renderWidth = Math.max(1.0, Math.ceil(boundsWidth * effectiveScale));
        double renderHeight = Math.max(1.0, Math.ceil(boundsHeight * effectiveScale));
        double offscreenWidth = renderWidth + padding * 2;
        double offscreenHeight = renderHeight + padding * 2;
        logger.debug("Creating offscreen buffer: {}x{} (render={}x{}, padding={}, scale={})",
                offscreenWidth, offscreenHeight, renderWidth, renderHeight, padding, effectiveScale);

        WritableImage blurredImage = OffscreenRenderer.renderToImage(offscreenWidth, offscreenHeight, offscreenGc -> {
            offscreenGc.save();
            offscreenGc.scale(effectiveScale, effectiveScale);
            offscreenGc.translate(padding / effectiveScale, padding / effectiveScale);
            applyBlurEffect(offscreenGc, scaledBlurRadius);
            layerRenderer.render(offscreenGc, layer, frame);
            offscreenGc.setEffect(null);
            offscreenGc.restore();
        });

        gc.save();
        gc.drawImage(blurredImage,
                padding, padding, renderWidth, renderHeight,
                0, 0, boundsWidth, boundsHeight);
        gc.restore();

        logger.debug("Applied bounded blur crop for layer {}: radius={} (scaled={}) src=({}, {}, {}, {}) dst=(0, 0, {}, {})",
                layer.name(), blurRadius, scaledBlurRadius, padding, padding, renderWidth, renderHeight, boundsWidth, boundsHeight);
    }

    /**
     * Chooses and installs a JavaFX blur effect for the supplied radius.
     *
     * <p>Smaller radii use {@link GaussianBlur} for smoother results, while larger radii switch to
     * {@link BoxBlur} with additional iterations to approximate heavier blur at lower cost.
     * The chosen effect is written directly to {@code gc} and remains active until the caller clears or
     * restores it.
     *
     * @param gc         graphics context that receives the blur effect
     * @param blurRadius blur radius already mapped into JavaFX space
     */
    private void applyBlurEffect(GraphicsContext gc, double blurRadius) {
        if (blurRadius > 40) {
            gc.setEffect(new BoxBlur(blurRadius, blurRadius, 3));
        } else if (blurRadius > 20) {
            gc.setEffect(new BoxBlur(blurRadius, blurRadius, 2));
        } else {
            gc.setEffect(new GaussianBlur(blurRadius));
        }
    }

    /**
     * Determines whether a layer is safe and worthwhile to rasterize once and reuse as a blurred image.
     *
     * <p>The cache is intentionally conservative. A layer qualifies only when the blur is large enough to be
     * expensive, the layer has no matte or mask state that could change compositing per frame, and no animated
     * values are found anywhere in the inspected layer model subtree.
     *
     * @param layer      layer to evaluate; {@code null} returns {@code false}
     * @param blurRadius sampled blur radius for the current frame
     * @return {@code true} when the layer can be treated as frame-invariant for blurred raster reuse
     */
    public boolean canUseStaticBlurLayerCache(Layer layer, double blurRadius) {
        return layer != null
                && blurRadius >= STATIC_BLUR_CACHE_MIN_RADIUS
                && layer.matteMode() == null
                && (layer.matteTarget() == null || layer.matteTarget() != 1)
                && (layer.masks() == null || layer.masks().isEmpty())
                && !containsAnimation(layer);
    }

    /**
     * Recursively inspects a model subtree for animation markers.
     *
     * <p>The traversal recognizes {@link Animated}, {@link AnimatedBezier}, iterables, arrays, and record
     * components. Objects are tracked by identity so cyclic graphs can be visited safely.
     *
     * @param value root value to inspect; may be {@code null}
     * @return {@code true} if any visited descendant is explicitly animated, otherwise {@code false}
     */
    public boolean containsAnimation(Object value) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        return containsAnimation(value, visited);
    }

    /**
     * Renders a blur-heavy static layer into a cached image and reuses that image across later frames.
     *
     * <p>If the requested bounds are invalid or the raster would exceed the configured pixel budget, the method
     * falls back to live blur rendering. Otherwise, it caches the blurred image by layer identity, integer
     * raster size, and blur radius bits, then draws the cached image at the current origin of {@code gc}.
     *
     * @param gc            destination graphics context that receives the cached image
     * @param layer         layer to render and cache
     * @param frame         frame used when the cache entry is first rendered
     * @param blurRadius    JavaFX blur radius to bake into the cached image
     * @param boundsWidth   local raster width for the cached image
     * @param boundsHeight  local raster height for the cached image
     * @param layerRenderer callback that renders the original layer content into the cache image
     */
    public void renderStaticLayerWithGaussianBlurCache(GraphicsContext gc,
                                                       Layer layer,
                                                       double frame,
                                                       double blurRadius,
                                                       double boundsWidth,
                                                       double boundsHeight,
                                                       LayerRenderer layerRenderer) {
        renderStaticLayerWithGaussianBlurCache(gc, layer, frame, blurRadius, boundsWidth, boundsHeight, 1.0, layerRenderer);
    }

    /**
     * Renders and caches a blurred static layer using a configurable off-screen resolution scale.
     *
     * @param gc                    destination graphics context
     * @param layer                 layer to render
     * @param frame                 frame sampled for initial cache creation
     * @param blurRadius            blur radius in composition-space pixels
     * @param boundsWidth           destination width in composition-space coordinates
     * @param boundsHeight          destination height in composition-space coordinates
     * @param renderResolutionScale off-screen raster scale factor in range {@code (0, 1]}
     * @param layerRenderer         callback to render layer content
     */
    public void renderStaticLayerWithGaussianBlurCache(GraphicsContext gc,
                                                       Layer layer,
                                                       double frame,
                                                       double blurRadius,
                                                       double boundsWidth,
                                                       double boundsHeight,
                                                       double renderResolutionScale,
                                                       LayerRenderer layerRenderer) {
        double effectiveScale = Math.clamp(renderResolutionScale, 0.1, 1.0);
        int imageWidth = Math.max(1, (int) Math.ceil(boundsWidth * effectiveScale));
        int imageHeight = Math.max(1, (int) Math.ceil(boundsHeight * effectiveScale));
        double scaledBlurRadius = blurRadius * effectiveScale;
        long pixelCount = (long) imageWidth * imageHeight;
        if (boundsWidth <= 0 || boundsHeight <= 0 || pixelCount > STATIC_BLUR_CACHE_MAX_PIXEL_COUNT) {
            logger.debug("Skipping static blur cache for layer {} - bounds={}x{}, pixels={}, scale={}",
                    layer.name(), boundsWidth, boundsHeight, pixelCount, effectiveScale);
            renderLayerWithGaussianBlur(gc, layer, frame, blurRadius, boundsWidth, boundsHeight, effectiveScale, layerRenderer);
            return;
        }

        StaticLayerBlurCacheKey cacheKey = new StaticLayerBlurCacheKey(
                System.identityHashCode(layer),
                imageWidth,
                imageHeight,
                Double.doubleToLongBits(scaledBlurRadius)
        );

        WritableImage cachedImage = staticLayerBlurCache.computeIfAbsent(cacheKey,
                unused -> createStaticBlurCacheImage(layer, frame, scaledBlurRadius, imageWidth, imageHeight, effectiveScale, layerRenderer));
        gc.drawImage(cachedImage,
                0, 0, imageWidth, imageHeight,
                0, 0, boundsWidth, boundsHeight);
    }

    /**
     * Creates a new cached blurred raster for a layer.
     *
     * <p>The layer is rendered once into an off-screen image of the requested size with the sampled blur already
     * applied. Callers are responsible for cache-key selection and reuse.
     *
     * @param layer         layer to rasterize
     * @param frame         frame sampled while producing the image
     * @param blurRadius    JavaFX blur radius to bake into the image
     * @param imageWidth    cache image width in pixels
     * @param imageHeight   cache image height in pixels
     * @param layerRenderer callback that renders the layer into the off-screen context
     * @return the blurred raster image ready to be drawn back onto the destination context
     */
    private WritableImage createStaticBlurCacheImage(Layer layer,
                                                     double frame,
                                                     double blurRadius,
                                                     int imageWidth,
                                                     int imageHeight,
                                                     double renderResolutionScale,
                                                     LayerRenderer layerRenderer) {
        logger.debug("Creating static blur cache image for layer {}: {}x{}, radius={}, scale={}",
                layer.name(), imageWidth, imageHeight, blurRadius, renderResolutionScale);
        return OffscreenRenderer.renderToImage(imageWidth, imageHeight, offscreenGc -> {
            offscreenGc.save();
            offscreenGc.scale(renderResolutionScale, renderResolutionScale);
            applyBlurEffect(offscreenGc, blurRadius);
            layerRenderer.render(offscreenGc, layer, frame);
            offscreenGc.setEffect(null);
            offscreenGc.restore();
        });
    }

    /**
     * Internal recursive implementation of {@link #containsAnimation(Object)}.
     *
     * @param value   current node being inspected
     * @param visited identity set used to prevent infinite recursion through shared or cyclic object graphs
     * @return {@code true} if an animated descendant is found
     */
    private boolean containsAnimation(Object value, Set<Object> visited) {
        if (value == null || isTriviallyStaticValue(value)) {
            return false;
        }
        if (!visited.add(value)) {
            return false;
        }
        if (value instanceof Animated animated) {
            return (animated.animated() != null && animated.animated() > 0)
                    || containsAnimation(animated.x(), visited)
                    || containsAnimation(animated.y(), visited);
        }
        if (value instanceof AnimatedBezier animatedBezier) {
            return animatedBezier.animated() != null && animatedBezier.animated() > 0;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                if (containsAnimation(entry, visited)) {
                    return true;
                }
            }
            return false;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                if (containsAnimation(Array.get(value, i), visited)) {
                    return true;
                }
            }
            return false;
        }
        if (!value.getClass().isRecord()) {
            return false;
        }
        for (RecordComponent component : value.getClass().getRecordComponents()) {
            try {
                if (containsAnimation(component.getAccessor().invoke(value), visited)) {
                    return true;
                }
            } catch (ReflectiveOperationException ex) {
                logger.debug("Unable to inspect record component {} on {}",
                        component.getName(), value.getClass().getSimpleName(), ex);
            }
        }
        return false;
    }

    /**
     * Returns whether a value can be treated as inherently non-animated without further traversal.
     *
     * @param value value to classify
     * @return {@code true} for primitive-like scalar wrappers and enums that cannot contain nested animation data
     */
    private boolean isTriviallyStaticValue(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value instanceof Enum<?>;
    }

    /**
     * Callback interface used to render layer content into either the live graphics context or an off-screen
     * buffer managed by {@link EffectsRenderer}.
     */
    @FunctionalInterface
    public interface LayerRenderer {
        /**
         * Draws the layer content for a single frame.
         *
         * <p>The callback should render only the layer's own pixels. Any required blur effect, padding offset,
         * or cache target selection is handled by the caller.
         *
         * @param gc    graphics context to draw into
         * @param layer layer being rendered
         * @param frame animation frame to sample
         */
        void render(GraphicsContext gc, Layer layer, double frame);
    }

    /**
     * Cache key for a blurred static layer raster.
     *
     * @param layerIdentityHash identity-based hash of the source layer instance
     * @param width             cached image width in pixels
     * @param height            cached image height in pixels
     * @param blurBits          raw {@code double} bits for the sampled blur radius
     */
    private record StaticLayerBlurCacheKey(int layerIdentityHash, int width, int height, long blurBits) {
    }
}
