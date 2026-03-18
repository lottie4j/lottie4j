package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.model.animation.Animation;
import com.lottie4j.core.model.asset.Asset;
import com.lottie4j.core.model.layer.Layer;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.fxplayer.util.FrameTiming;
import com.lottie4j.fxplayer.util.OffscreenRenderer;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.WritableImage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Renderer for precomposition layers in Lottie animations.
 * <p>
 * A precomposition (precomp) is a nested composition that contains its own set of layers
 * referenced from the animation's asset collection. This renderer handles the recursive
 * rendering of precomp layers, including:
 * <ul>
 * <li>Resolving precomp assets and their child layers</li>
 * <li>Managing local frame timing within nested compositions</li>
 * <li>Applying parent transform hierarchies</li>
 * <li>Handling different layer types (shape, image, text, solid color, nested precomps)</li>
 * <li>Applying effects such as Gaussian blur</li>
 * <li>Clipping content to precomp boundaries</li>
 * </ul>
 * <p>
 * The renderer delegates specific layer type rendering to callback interfaces, allowing
 * flexible integration with different rendering strategies.
 */
public class PrecompRenderer {

    private static final Logger logger = LoggerFactory.getLogger(PrecompRenderer.class);

    private final TransformApplier transformApplier;
    private final TextRenderer textRenderer;
    private final ImageRenderer imageRenderer;
    private final EffectsRenderer effectsRenderer;
    private final MatteRenderer matteRenderer;
    private final Map<String, PrecompRenderCache> precompRenderCacheByAssetId = new HashMap<>();

    /**
     * Creates a precomposition renderer with required collaborators.
     *
     * @param transformApplier helper used to apply layer and parent transforms
     * @param textRenderer     renderer used for text layers inside precomps
     * @param imageRenderer    renderer used for image layers inside precomps
     */
    public PrecompRenderer(TransformApplier transformApplier, TextRenderer textRenderer, ImageRenderer imageRenderer) {
        this.transformApplier = transformApplier;
        this.textRenderer = textRenderer;
        this.imageRenderer = imageRenderer;
        this.effectsRenderer = new EffectsRenderer();
        this.matteRenderer = new MatteRenderer();
    }

    /**
     * Renders the content of a precomposition layer by resolving its asset and traversing child layers.
     *
     * @param gc                      destination graphics context
     * @param layer                   precomposition layer
     * @param frame                   frame in parent timeline
     * @param assetsById              asset lookup map
     * @param animation               root animation, used for nested image layers
     * @param layerActivityEvaluator  callback used to evaluate layer in/out activity
     * @param solidColorLayerRenderer callback to render solid color layers
     * @param shapeGroupRenderer      callback to render grouped shapes
     * @param shapeRendererDelegate   callback to render non-group shapes
     */
    public void renderPrecompositionLayer(GraphicsContext gc,
                                          Layer layer,
                                          double frame,
                                          Map<String, Asset> assetsById,
                                          Animation animation,
                                          LayerActivityEvaluator layerActivityEvaluator,
                                          SolidColorLayerRenderer solidColorLayerRenderer,
                                          ShapeGroupRenderer shapeGroupRenderer,
                                          ShapeRendererDelegate shapeRendererDelegate) {
        renderPrecompositionLayer(gc,
                layer,
                frame,
                assetsById,
                animation,
                1.0,
                layerActivityEvaluator,
                solidColorLayerRenderer,
                shapeGroupRenderer,
                shapeRendererDelegate);
    }

    /**
     * Renders a precomposition layer with a configurable off-screen resolution scale.
     *
     * @param gc                      destination graphics context
     * @param layer                   precomposition layer
     * @param frame                   frame in parent timeline
     * @param assetsById              asset lookup map
     * @param animation               root animation, used for nested image layers
     * @param renderResolutionScale   render resolution scale for off-screen buffers
     * @param layerActivityEvaluator  callback used to evaluate layer in/out activity
     * @param solidColorLayerRenderer callback to render solid color layers
     * @param shapeGroupRenderer      callback to render grouped shapes
     * @param shapeRendererDelegate   callback to render non-group shapes
     */
    public void renderPrecompositionLayer(GraphicsContext gc,
                                          Layer layer,
                                          double frame,
                                          Map<String, Asset> assetsById,
                                          Animation animation,
                                          double renderResolutionScale,
                                          LayerActivityEvaluator layerActivityEvaluator,
                                          SolidColorLayerRenderer solidColorLayerRenderer,
                                          ShapeGroupRenderer shapeGroupRenderer,
                                          ShapeRendererDelegate shapeRendererDelegate) {
        renderPrecompositionLayer(gc,
                layer,
                frame,
                assetsById,
                animation,
                -1,
                -1,
                Math.clamp(renderResolutionScale, 0.1, 1.0),
                layerActivityEvaluator,
                solidColorLayerRenderer,
                shapeGroupRenderer,
                shapeRendererDelegate);
    }

    private void renderPrecompositionLayer(GraphicsContext gc,
                                           Layer layer,
                                           double frame,
                                           Map<String, Asset> assetsById,
                                           Animation animation,
                                           double inheritedWidth,
                                           double inheritedHeight,
                                           double renderResolutionScale,
                                           LayerActivityEvaluator layerActivityEvaluator,
                                           SolidColorLayerRenderer solidColorLayerRenderer,
                                           ShapeGroupRenderer shapeGroupRenderer,
                                           ShapeRendererDelegate shapeRendererDelegate) {
        if (layer.referenceId() == null) {
            logger.warn("Precomposition layer has no referenceId: {}", layer.name());
            return;
        }

        Asset asset = assetsById.get(layer.referenceId());
        if (asset == null) {
            logger.warn("Asset not found for referenceId: {}", layer.referenceId());
            return;
        }

        if (asset.layers() == null || asset.layers().isEmpty()) {
            logger.warn("Precomposition asset has no layers: {}", layer.referenceId());
            return;
        }

        double localFrame = FrameTiming.toLocalFrame(layer, frame);
        double precompWidth = resolvePrecompWidth(layer, asset, animation, inheritedWidth);
        double precompHeight = resolvePrecompHeight(layer, asset, animation, inheritedHeight);
        logger.debug("Rendering precomposition: {} with {} layers at localFrame={} (global={}), bounds={}x{} (inherited={}x{})",
                layer.referenceId(), asset.layers().size(), localFrame, frame, precompWidth, precompHeight, inheritedWidth, inheritedHeight);

        PrecompRenderCache precompRenderCache = getOrBuildPrecompRenderCache(asset);

        gc.save();

        if (precompWidth > 0 && precompHeight > 0) {
            // Clip all child rendering to precomp bounds (including blurred layers).
            logger.debug("Setting precomp clip for {}: rect(0,0,{},{})", layer.referenceId(), precompWidth, precompHeight);
            gc.beginPath();
            gc.rect(0, 0, precompWidth, precompHeight);
            gc.clip();
        } else {
            logger.debug("No precomp clip for {} - bounds {}x{}", layer.referenceId(), precompWidth, precompHeight);
        }

        List<Layer> precompLayers = asset.layers();
        for (int i = precompLayers.size() - 1; i >= 0; i--) {
            Layer precompLayer = precompLayers.get(i);
            if (!layerActivityEvaluator.isActive(precompLayer, localFrame)) {
                continue;
            }
            logger.debug("Rendering precomp layer [{}] ind={} name='{}'", i, precompLayer.indexLayer(), precompLayer.name());

            Optional<Layer> matteSource = resolveActiveMatteSource(precompLayers, i, localFrame, layerActivityEvaluator);
            if (precompLayer.matteMode() != null && matteSource.isPresent()) {
                Layer resolvedMatteSource = matteSource.get();
                logger.debug("Rendering matted precomp layer {} with matte from {}", precompLayer.name(), resolvedMatteSource.name());
                matteRenderer.renderLayerWithMatte(
                        gc,
                        precompLayer,
                        resolvedMatteSource,
                        localFrame,
                        (int) Math.max(1, precompWidth),
                        (int) Math.max(1, precompHeight),
                        renderResolutionScale,
                        (matteGc, matteLayer, matteFrame) -> renderPrecompLayer(
                                matteGc,
                                matteLayer,
                                matteFrame,
                                precompRenderCache,
                                assetsById,
                                animation,
                                precompWidth,
                                precompHeight,
                                renderResolutionScale,
                                layerActivityEvaluator,
                                solidColorLayerRenderer,
                                shapeGroupRenderer,
                                shapeRendererDelegate
                        )
                );
                continue;
            }

            if (precompLayer.matteTarget() != null && precompLayer.matteTarget() == 1) {
                logger.debug("Skipping matte source layer in precomp: {}", precompLayer.name());
                continue;
            }

            if (shouldSkipDueToParentMatte(precompLayer, precompRenderCache)) {
                logger.debug("Skipping precomp layer with skipped parent: {}", precompLayer.name());
                continue;
            }

            renderPrecompLayer(gc,
                    precompLayer,
                    localFrame,
                    precompRenderCache,
                    assetsById,
                    animation,
                    precompWidth,
                    precompHeight,
                    renderResolutionScale,
                    layerActivityEvaluator,
                    solidColorLayerRenderer,
                    shapeGroupRenderer,
                    shapeRendererDelegate);
        }

        gc.restore();
    }

    /**
     * Renders a single layer inside a precomposition, including recursive nested precomps.
     *
     * @param gc                      graphics context
     * @param layer                   precomp child layer
     * @param frame                   local precomp frame
     * @param precompRenderCache      cached layer metadata for this precomp asset
     * @param assetsById              asset lookup map
     * @param animation               root animation
     * @param precompWidth            precomp width for clipping
     * @param precompHeight           precomp height for clipping
     * @param layerActivityEvaluator  layer activity callback
     * @param solidColorLayerRenderer solid color renderer callback
     * @param shapeGroupRenderer      shape group renderer callback
     * @param shapeRendererDelegate   shape renderer callback
     */
    private void renderPrecompLayer(GraphicsContext gc,
                                    Layer layer,
                                    double frame,
                                    PrecompRenderCache precompRenderCache,
                                    Map<String, Asset> assetsById,
                                    Animation animation,
                                    double precompWidth,
                                    double precompHeight,
                                    double renderResolutionScale,
                                    LayerActivityEvaluator layerActivityEvaluator,
                                    SolidColorLayerRenderer solidColorLayerRenderer,
                                    ShapeGroupRenderer shapeGroupRenderer,
                                    ShapeRendererDelegate shapeRendererDelegate) {
        // Check for Gaussian Blur effect and render with blur if needed
        double blurRadius = effectsRenderer.getGaussianBlurRadius(layer, frame);
        if (blurRadius > 0.0) {
            logger.debug("Rendering blurred layer {} in precomp with bounds {}x{}", layer.name(), precompWidth, precompHeight);

            // Check layer opacity - skip rendering if transparent
            if (layer.transform() != null && layer.transform().opacity() != null) {
                double opacity = layer.transform().opacity().getValue(0, frame);
                if (opacity <= 0) {
                    logger.debug("Skipping blurred precomp layer {} - opacity is {}", layer.name(), opacity);
                    return;
                }
            }

            EffectsRenderer.LayerRenderer precompBlurRenderer = (blurGc, blurLayer, blurFrame) -> {
                applyPrecompParentTransforms(blurGc, blurLayer, blurFrame, precompRenderCache);
                transformApplier.applyLayerTransform(blurGc, blurLayer, blurFrame);
                renderPrecompLayerContentOnly(blurGc, blurLayer, blurFrame, animation,
                        solidColorLayerRenderer, shapeGroupRenderer, shapeRendererDelegate, precompRenderCache);
            };

            if (shouldUseStaticBlurLayerCache(layer, blurRadius, precompWidth, precompHeight, precompRenderCache)) {
                effectsRenderer.renderStaticLayerWithGaussianBlurCache(
                        gc,
                        layer,
                        frame,
                        blurRadius,
                        Math.max(1.0, precompWidth),
                        Math.max(1.0, precompHeight),
                        renderResolutionScale,
                        precompBlurRenderer
                );
            } else {
                effectsRenderer.renderLayerWithGaussianBlur(gc,
                        layer,
                        frame,
                        blurRadius,
                        precompWidth,
                        precompHeight,
                        renderResolutionScale,
                        precompBlurRenderer);
            }
            return;
        }

        renderPrecompLayerInternal(gc, layer, frame, precompRenderCache, assetsById, animation,
                precompWidth, precompHeight, renderResolutionScale, layerActivityEvaluator,
                solidColorLayerRenderer, shapeGroupRenderer, shapeRendererDelegate);
    }

    private boolean shouldUseStaticBlurLayerCache(Layer layer,
                                                  double blurRadius,
                                                  double precompWidth,
                                                  double precompHeight,
                                                  PrecompRenderCache precompRenderCache) {
        return precompWidth > 0
                && precompHeight > 0
                && effectsRenderer.canUseStaticBlurLayerCache(layer, blurRadius)
                && !hasAnimatedParentTransformChain(layer, precompRenderCache);
    }

    private boolean hasAnimatedParentTransformChain(Layer layer, PrecompRenderCache precompRenderCache) {
        return precompRenderCache.animatedParentTransformByLayer()
                .computeIfAbsent(layer, current -> {
                    for (Layer parent : precompRenderCache.parentChainByLayer().getOrDefault(current, List.of())) {
                        if (effectsRenderer.containsAnimation(parent.transform())) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    /**
     * Internal rendering method for precomp layers (without blur handling).
     *
     * @param gc                      graphics context
     * @param layer                   precomp child layer
     * @param frame                   local precomp frame
     * @param precompRenderCache      cached layer metadata for this precomp asset
     * @param assetsById              asset lookup map
     * @param animation               root animation
     * @param parentPrecompWidth      inherited precomp width for nested precomps
     * @param parentPrecompHeight     inherited precomp height for nested precomps
     * @param renderResolutionScale   render resolution scale for off-screen buffers
     * @param layerActivityEvaluator  layer activity callback
     * @param solidColorLayerRenderer solid color renderer callback
     * @param shapeGroupRenderer      shape group renderer callback
     * @param shapeRendererDelegate   shape renderer callback
     */
    private void renderPrecompLayerInternal(GraphicsContext gc,
                                            Layer layer,
                                            double frame,
                                            PrecompRenderCache precompRenderCache,
                                            Map<String, Asset> assetsById,
                                            Animation animation,
                                            double parentPrecompWidth,
                                            double parentPrecompHeight,
                                            double renderResolutionScale,
                                            LayerActivityEvaluator layerActivityEvaluator,
                                            SolidColorLayerRenderer solidColorLayerRenderer,
                                            ShapeGroupRenderer shapeGroupRenderer,
                                            ShapeRendererDelegate shapeRendererDelegate) {
        // Check layer opacity first - skip rendering if transparent
        double layerOpacity = 1.0;
        if (layer.transform() != null && layer.transform().opacity() != null) {
            layerOpacity = layer.transform().opacity().getValue(0, frame) / 100.0;
            if (layerOpacity <= 0) {
                logger.debug("Skipping precomp layer {} - opacity is {}", layer.name(), layerOpacity);
                return;
            }
        }

        // If layer has a non-normal blend mode, we need to render to offscreen buffer first
        // then composite with the blend mode (matching HTML/CSS behavior)
        if (layer.blendMode() != null && layer.blendMode() != com.lottie4j.core.definition.BlendMode.NORMAL) {
            renderLayerWithBlendMode(gc, layer, frame, precompRenderCache, assetsById, animation,
                    parentPrecompWidth, parentPrecompHeight, renderResolutionScale,
                    layerActivityEvaluator, solidColorLayerRenderer, shapeGroupRenderer, shapeRendererDelegate);
            return;
        }

        // Delegate to internal method that does actual rendering
        renderPrecompLayerWithoutBlendMode(gc, layer, frame, precompRenderCache, assetsById, animation,
                parentPrecompWidth, parentPrecompHeight, renderResolutionScale,
                layerActivityEvaluator, solidColorLayerRenderer, shapeGroupRenderer, shapeRendererDelegate);
    }

    /**
     * Renders shape content for a non-null precomp layer.
     *
     * @param gc                    destination graphics context
     * @param layer                 precomp child layer containing shapes
     * @param frame                 local precomp frame
     * @param shapeGroupRenderer    callback used for group shapes
     * @param shapeRendererDelegate callback used for direct shape primitives
     */
    private void renderPrecompShapes(GraphicsContext gc,
                                     Layer layer,
                                     double frame,
                                     ShapeGroupRenderer shapeGroupRenderer,
                                     ShapeRendererDelegate shapeRendererDelegate,
                                     PrecompRenderCache precompRenderCache) {
        if (layer.shapes() == null || layer.shapes().isEmpty()) {
            logger.debug("Layer has no shapes");
            return;
        }

        logger.debug("Layer has {} shapes", layer.shapes().size());

        TrimPath layerTrimPath = precompRenderCache.layerTrimPathByLayer().get(layer);

        List<BaseShape> shapes = layer.shapes();
        for (int i = shapes.size() - 1; i >= 0; i--) {
            BaseShape shape = shapes.get(i);
            if (shape instanceof TrimPath) {
                continue;
            }

            switch (shape.shapeType().shapeGroup()) {
                case GROUP -> shapeGroupRenderer.render(gc, shape, frame, layerTrimPath);
                case SHAPE -> shapeRendererDelegate.render(shape, null, frame);
                case STYLE -> {
                    // Styles are consumed by parent group renderers.
                }
                case MODIFIER -> {
                    // Modifiers are consumed by group-level rendering passes.
                }
                default -> logger.debug("Unsupported shape type: {}", shape.shapeType().shapeGroup());
            }
        }
    }

    /**
     * Renders only the content of a precomp layer (shapes/images) without applying transforms.
     * Used for blur rendering where transforms are applied externally.
     *
     * @param gc                      graphics context
     * @param layer                   precomp layer
     * @param frame                   local precomp frame
     * @param animation               root animation
     * @param solidColorLayerRenderer solid color renderer callback
     * @param shapeGroupRenderer      shape group renderer callback
     * @param shapeRendererDelegate   shape renderer callback
     */
    private void renderPrecompLayerContentOnly(GraphicsContext gc,
                                               Layer layer,
                                               double frame,
                                               Animation animation,
                                               SolidColorLayerRenderer solidColorLayerRenderer,
                                               ShapeGroupRenderer shapeGroupRenderer,
                                               ShapeRendererDelegate shapeRendererDelegate,
                                               PrecompRenderCache precompRenderCache) {
        logger.debug("renderPrecompLayerContentOnly called for {} type={}", layer.name(), layer.layerType());
        if (layer.layerType() == LayerType.PRECOMPOSITION) {
            logger.warn("Nested precomp blur not supported for content-only rendering: {}", layer.name());
        } else if (layer.layerType() == LayerType.IMAGE) {
            imageRenderer.render(gc, layer, animation);
        } else if (layer.layerType() == LayerType.SOLD_COLOR) {
            solidColorLayerRenderer.render(gc, layer);
        } else if (layer.layerType() == LayerType.TEXT) {
            textRenderer.render(gc, layer, frame);
        } else if (layer.layerType() != LayerType.NULL) {
            logger.debug("Rendering shapes for layer {} - has {} shapes", layer.name(),
                    layer.shapes() != null ? layer.shapes().size() : 0);
            renderPrecompShapes(gc, layer, frame, shapeGroupRenderer, shapeRendererDelegate, precompRenderCache);
        } else {
            logger.debug("Skipping shape rendering for NULL layer: {}", layer.name());
        }
    }

    /**
     * Recursively applies parent transforms for a precomp child layer.
     *
     * @param gc                 destination graphics context
     * @param layer              child layer whose parent chain is applied
     * @param frame              local precomp frame
     * @param precompRenderCache cached layer metadata for this precomp asset
     */
    private void applyPrecompParentTransforms(GraphicsContext gc,
                                              Layer layer,
                                              double frame,
                                              PrecompRenderCache precompRenderCache) {
        for (Layer parent : precompRenderCache.parentChainByLayer().getOrDefault(layer, List.of())) {
            transformApplier.applyLayerTransformWithoutOpacity(gc, parent, frame);
        }
    }

    /**
     * Resolves the effective width of a precomposition from layer, asset, or animation fallbacks.
     *
     * @param layer          precomp layer
     * @param asset          precomp asset
     * @param animation      root animation
     * @param inheritedWidth width inherited from parent precomp
     * @return resolved width, or -1 if unavailable
     */
    private double resolvePrecompWidth(Layer layer, Asset asset, Animation animation, double inheritedWidth) {
        if (layer.width() != null && layer.width() > 0) {
            return layer.width();
        }
        if (asset.width() != null && asset.width() > 0) {
            return asset.width();
        }
        if (inheritedWidth > 0) {
            logger.debug("Using inherited precomp width {} for {}", inheritedWidth, layer.referenceId());
            return inheritedWidth;
        }
        if (animation.width() != null && animation.width() > 0) {
            logger.debug("Using root animation width {} as fallback for precomp {}", animation.width(), layer.referenceId());
            return animation.width();
        }
        return -1;
    }

    /**
     * Resolves the effective height of a precomposition from layer, asset, or animation fallbacks.
     *
     * @param layer           precomp layer
     * @param asset           precomp asset
     * @param animation       root animation
     * @param inheritedHeight height inherited from parent precomp
     * @return resolved height, or -1 if unavailable
     */
    private double resolvePrecompHeight(Layer layer, Asset asset, Animation animation, double inheritedHeight) {
        if (layer.height() != null && layer.height() > 0) {
            return layer.height();
        }

        // Asset height is now modeled as Integer
        if (asset.height() != null && asset.height() > 0) {
            return asset.height();
        }

        if (inheritedHeight > 0) {
            logger.debug("Using inherited precomp height {} for {}", inheritedHeight, layer.referenceId());
            return inheritedHeight;
        }

        if (animation.height() != null && animation.height() > 0) {
            logger.debug("Using root animation height {} as fallback for precomp {}", animation.height(), layer.referenceId());
            return animation.height();
        }

        return -1;
    }

    private Optional<Layer> resolveActiveMatteSource(List<Layer> layers,
                                                     int matteUserIndex,
                                                     double frame,
                                                     LayerActivityEvaluator layerActivityEvaluator) {
        if (matteUserIndex > 0) {
            Layer previous = layers.get(matteUserIndex - 1);
            if (isMatteSource(previous, frame, layerActivityEvaluator)) {
                return Optional.of(previous);
            }
        }
        if (matteUserIndex + 1 < layers.size()) {
            Layer next = layers.get(matteUserIndex + 1);
            if (isMatteSource(next, frame, layerActivityEvaluator)) {
                return Optional.of(next);
            }
        }
        return Optional.empty();
    }

    private boolean isMatteSource(Layer layer, double frame, LayerActivityEvaluator layerActivityEvaluator) {
        return layer != null
                && layer.matteTarget() != null
                && layer.matteTarget() == 1
                && layerActivityEvaluator.isActive(layer, frame);
    }

    private boolean shouldSkipDueToParentMatte(Layer layer, PrecompRenderCache precompRenderCache) {
        return precompRenderCache.skipDueToParentByLayer().getOrDefault(layer, false);
    }

    private PrecompRenderCache getOrBuildPrecompRenderCache(Asset asset) {
        String cacheKey = asset.id() != null ? asset.id() : "__anonymous_precomp__";
        return precompRenderCacheByAssetId.computeIfAbsent(cacheKey, key -> {
            Map<Integer, Layer> layersByIndex = new HashMap<>();
            Map<Layer, List<Layer>> parentChainByLayer = new IdentityHashMap<>();
            Map<Layer, Boolean> skipDueToParentByLayer = new IdentityHashMap<>();
            Map<Layer, TrimPath> layerTrimPathByLayer = new IdentityHashMap<>();
            Map<Layer, Boolean> animatedParentTransformByLayer = new IdentityHashMap<>();

            List<Layer> layers = asset.layers() != null ? asset.layers() : List.of();
            for (Layer candidate : layers) {
                if (candidate.indexLayer() != null) {
                    layersByIndex.put(candidate.indexLayer().intValue(), candidate);
                }
            }

            for (Layer candidate : layers) {
                layerTrimPathByLayer.put(candidate, findLayerTrimPath(candidate));
                List<Layer> parentChain = buildParentChain(candidate, layersByIndex);
                parentChainByLayer.put(candidate, parentChain);
                skipDueToParentByLayer.put(candidate, shouldSkipDueToParentMatte(parentChain));
            }

            return new PrecompRenderCache(
                    layersByIndex,
                    parentChainByLayer,
                    skipDueToParentByLayer,
                    layerTrimPathByLayer,
                    animatedParentTransformByLayer
            );
        });
    }

    private List<Layer> buildParentChain(Layer layer, Map<Integer, Layer> layersByIndex) {
        List<Layer> chain = new ArrayList<>();
        Integer parentIndex = layer.indexParent();
        int guard = 0;
        while (parentIndex != null && guard++ < layersByIndex.size()) {
            Layer parent = layersByIndex.get(parentIndex);
            if (parent == null) {
                break;
            }
            chain.add(parent);
            parentIndex = parent.indexParent();
        }
        Collections.reverse(chain);
        return chain;
    }

    private boolean shouldSkipDueToParentMatte(List<Layer> parentChain) {
        for (Layer parent : parentChain) {
            if (parent.matteMode() != null || (parent.matteTarget() != null && parent.matteTarget() == 1)) {
                return true;
            }
        }
        return false;
    }

    private TrimPath findLayerTrimPath(Layer layer) {
        if (layer.shapes() == null || layer.shapes().isEmpty()) {
            return null;
        }
        for (BaseShape shape : layer.shapes()) {
            if (shape instanceof TrimPath trimPath) {
                return trimPath;
            }
        }
        return null;
    }

    /**
     * Clears all cached precomp render metadata.
     */
    public void clearRenderCaches() {
        precompRenderCacheByAssetId.clear();
    }

    /**
     * Invalidates cached metadata for one precomp asset id.
     *
     * @param assetId precomp asset id
     */
    public void invalidateRenderCache(String assetId) {
        if (assetId == null) {
            return;
        }
        precompRenderCacheByAssetId.remove(assetId);
    }

    /**
     * Pre-builds caches for all precomp assets with layers.
     *
     * @param assetsById asset map from animation model
     */
    public void warmUpRenderCaches(Map<String, Asset> assetsById) {
        if (assetsById == null || assetsById.isEmpty()) {
            return;
        }

        for (Asset asset : assetsById.values()) {
            if (asset != null && asset.layers() != null && !asset.layers().isEmpty()) {
                getOrBuildPrecompRenderCache(asset);
            }
        }
    }

    /**
     * Callback for checking whether a layer should render at a frame.
     */
    @FunctionalInterface
    public interface LayerActivityEvaluator {
        /**
         * Determines whether the layer is active at the provided frame.
         *
         * @param layer layer to inspect
         * @param frame frame to evaluate
         * @return {@code true} if the layer should render
         */
        boolean isActive(Layer layer, double frame);
    }

    /**
     * Callback for rendering solid-color layers.
     */
    @FunctionalInterface
    public interface SolidColorLayerRenderer {
        /**
         * Renders a solid-color layer.
         *
         * @param gc    destination graphics context
         * @param layer layer to render
         */
        void render(GraphicsContext gc, Layer layer);
    }

    /**
     * Callback for rendering grouped shape content.
     */
    @FunctionalInterface
    public interface ShapeGroupRenderer {
        /**
         * Renders a grouped shape item.
         *
         * @param gc            destination graphics context
         * @param shape         group shape item
         * @param frame         frame to sample
         * @param layerTrimPath optional trim path inherited from layer context
         */
        void render(GraphicsContext gc, BaseShape shape, double frame, TrimPath layerTrimPath);
    }

    /**
     * Callback for rendering primitive shapes.
     */
    @FunctionalInterface
    public interface ShapeRendererDelegate {
        /**
         * Renders a shape primitive.
         *
         * @param shape       shape primitive to render
         * @param parentGroup parent group carrying styles/modifiers, or null
         * @param frame       frame to sample
         */
        void render(BaseShape shape, Group parentGroup, double frame);
    }

    /**
     * Internal method that renders a precomp layer without checking/applying blend modes.
     * This is used both for normal rendering and for rendering to offscreen buffers.
     */
    private void renderPrecompLayerWithoutBlendMode(GraphicsContext gc,
                                                     Layer layer,
                                                     double frame,
                                                     PrecompRenderCache precompRenderCache,
                                                     Map<String, Asset> assetsById,
                                                     Animation animation,
                                                     double parentPrecompWidth,
                                                     double parentPrecompHeight,
                                                     double renderResolutionScale,
                                                     LayerActivityEvaluator layerActivityEvaluator,
                                                     SolidColorLayerRenderer solidColorLayerRenderer,
                                                     ShapeGroupRenderer shapeGroupRenderer,
                                                     ShapeRendererDelegate shapeRendererDelegate) {
        gc.save();

        // Check layer opacity - skip rendering if transparent
        if (layer.transform() != null && layer.transform().opacity() != null) {
            double opacity = layer.transform().opacity().getValue(0, frame);
            if (opacity <= 0) {
                logger.debug("Skipping precomp layer {} - opacity is {}", layer.name(), opacity);
                gc.restore();
                return;
            }
        }

        applyPrecompParentTransforms(gc, layer, frame, precompRenderCache);
        transformApplier.applyLayerTransform(gc, layer, frame);

        if (layer.layerType() == LayerType.PRECOMPOSITION) {
            renderPrecompositionLayer(gc,
                    layer,
                    frame,
                    assetsById,
                    animation,
                    parentPrecompWidth,
                    parentPrecompHeight,
                    renderResolutionScale,
                    layerActivityEvaluator,
                    solidColorLayerRenderer,
                    shapeGroupRenderer,
                    shapeRendererDelegate);
        } else if (layer.layerType() == LayerType.IMAGE) {
            imageRenderer.render(gc, layer, animation);
        } else if (layer.layerType() == LayerType.SOLD_COLOR) {
            solidColorLayerRenderer.render(gc, layer);
        } else if (layer.layerType() == LayerType.TEXT) {
            textRenderer.render(gc, layer, frame);
        } else if (layer.layerType() != LayerType.NULL) {
            renderPrecompShapes(gc, layer, frame, shapeGroupRenderer, shapeRendererDelegate, precompRenderCache);
        } else {
            logger.debug("Skipping shape rendering for NULL layer: {}", layer.name());
        }

        gc.restore();
    }

    /**
     * Renders a layer with a blend mode using offscreen buffer approach.
     * This matches HTML/CSS blend mode behavior where the layer is rendered first,
     * then composited with the blend mode.
     */
    private void renderLayerWithBlendMode(GraphicsContext gc,
                                          Layer layer,
                                          double frame,
                                          PrecompRenderCache precompRenderCache,
                                          Map<String, Asset> assetsById,
                                          Animation animation,
                                          double parentPrecompWidth,
                                          double parentPrecompHeight,
                                          double renderResolutionScale,
                                          LayerActivityEvaluator layerActivityEvaluator,
                                          SolidColorLayerRenderer solidColorLayerRenderer,
                                          ShapeGroupRenderer shapeGroupRenderer,
                                          ShapeRendererDelegate shapeRendererDelegate) {
        // Get blend mode
        javafx.scene.effect.BlendMode fxBlendMode = convertToFxBlendMode(layer.blendMode());
        if (fxBlendMode == null) {
            // Unsupported blend mode - render normally
            logger.debug("Unsupported blend mode {} for layer {}, rendering normally", layer.blendMode(), layer.name());
            renderPrecompLayerWithoutBlendMode(gc, layer, frame, precompRenderCache, assetsById, animation,
                    parentPrecompWidth, parentPrecompHeight, renderResolutionScale,
                    layerActivityEvaluator, solidColorLayerRenderer, shapeGroupRenderer, shapeRendererDelegate);
            return;
        }

        if (frame == 0.0) {
            logger.info("Layer '{}' rendering with blend mode {} using offscreen buffer", layer.name(), fxBlendMode);
        }

        // Calculate bounds for offscreen buffer - use precomp bounds if available
        double bufferWidth = Math.max(100, parentPrecompWidth > 0 ? parentPrecompWidth : 400);
        double bufferHeight = Math.max(100, parentPrecompHeight > 0 ? parentPrecompHeight : 400);

        // Render layer to offscreen buffer
        WritableImage layerImage = OffscreenRenderer.renderToImage(bufferWidth, bufferHeight, offscreenGc -> {
            // Render the layer normally to the offscreen buffer
            renderPrecompLayerWithoutBlendMode(offscreenGc, layer, frame, precompRenderCache, assetsById, animation,
                    parentPrecompWidth, parentPrecompHeight, renderResolutionScale,
                    layerActivityEvaluator, solidColorLayerRenderer, shapeGroupRenderer, shapeRendererDelegate);
        });

        // Composite the offscreen image with blend mode
        gc.save();
        gc.setGlobalBlendMode(fxBlendMode);
        gc.drawImage(layerImage, 0, 0);
        gc.restore();
    }

    /**
     * Converts Lottie blend mode to JavaFX blend mode.
     *
     * @param lottieBlendMode Lottie blend mode
     * @return JavaFX blend mode, or null if not supported
     */
    private javafx.scene.effect.BlendMode convertToFxBlendMode(com.lottie4j.core.definition.BlendMode lottieBlendMode) {
        return switch (lottieBlendMode) {
            case NORMAL -> javafx.scene.effect.BlendMode.SRC_OVER;
            case MULTIPLY -> javafx.scene.effect.BlendMode.MULTIPLY;
            case SCREEN -> javafx.scene.effect.BlendMode.SCREEN;
            case OVERLAY -> javafx.scene.effect.BlendMode.OVERLAY;
            case DARKEN -> javafx.scene.effect.BlendMode.DARKEN;
            case LIGHTEN -> javafx.scene.effect.BlendMode.LIGHTEN;
            case COLOR_DODGE -> javafx.scene.effect.BlendMode.COLOR_DODGE;
            case COLOR_BURN -> javafx.scene.effect.BlendMode.COLOR_BURN;
            case HARD_LIGHT -> null; // Not supported in JavaFX
            case SOFT_LIGHT -> null; // Not supported in JavaFX
            case DIFFERENCE -> javafx.scene.effect.BlendMode.DIFFERENCE;
            case EXCLUSION -> javafx.scene.effect.BlendMode.EXCLUSION;
            case HUE -> null; // Not supported in JavaFX
            case SATURATION -> null; // Not supported in JavaFX
            case COLOR -> null; // Not supported in JavaFX
            case LUMINOSITY -> null; // Not supported in JavaFX
        };
    }

    private record PrecompRenderCache(
            Map<Integer, Layer> layersByIndex,
            Map<Layer, List<Layer>> parentChainByLayer,
            Map<Layer, Boolean> skipDueToParentByLayer,
            Map<Layer, TrimPath> layerTrimPathByLayer,
            Map<Layer, Boolean> animatedParentTransformByLayer) {
    }
}
