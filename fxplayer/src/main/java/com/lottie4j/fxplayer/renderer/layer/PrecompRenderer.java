package com.lottie4j.fxplayer.renderer.layer;

import com.lottie4j.core.definition.LayerType;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Asset;
import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.BaseShape;
import com.lottie4j.core.model.shape.grouping.Group;
import com.lottie4j.core.model.shape.modifier.TrimPath;
import com.lottie4j.fxplayer.util.FrameTiming;
import javafx.scene.canvas.GraphicsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrecompRenderer {

    private static final Logger logger = LoggerFactory.getLogger(PrecompRenderer.class);

    private final TransformApplier transformApplier;
    private final TextRenderer textRenderer;
    private final ImageRenderer imageRenderer;
    private final EffectsRenderer effectsRenderer;

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
                -1,
                -1,
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

        Map<Integer, Layer> precompLayersByIndex = new HashMap<>();
        for (Layer precompLayer : asset.layers()) {
            if (precompLayer.indexLayer() != null) {
                precompLayersByIndex.put(precompLayer.indexLayer().intValue(), precompLayer);
            }
        }

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

        for (int i = asset.layers().size() - 1; i >= 0; i--) {
            Layer precompLayer = asset.layers().get(i);
            if (!layerActivityEvaluator.isActive(precompLayer, localFrame)) {
                continue;
            }
            logger.debug("Rendering precomp layer [{}] ind={} name='{}'", i, precompLayer.indexLayer(), precompLayer.name());

            renderPrecompLayer(gc,
                    precompLayer,
                    localFrame,
                    precompLayersByIndex,
                    assetsById,
                    animation,
                    precompWidth,
                    precompHeight,
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
     * @param precompLayersByIndex    layer lookup map
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
                                    Map<Integer, Layer> precompLayersByIndex,
                                    Map<String, Asset> assetsById,
                                    Animation animation,
                                    double precompWidth,
                                    double precompHeight,
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

            // Render with blur, passing transforms to be applied inside offscreen buffer
            // Then composite back with precomp clip active
            effectsRenderer.renderLayerWithGaussianBlur(gc, layer, frame, blurRadius, precompWidth, precompHeight,
                    (blurGc, blurLayer, blurFrame) -> {
                        // Apply transforms inside the offscreen buffer
                        applyPrecompParentTransforms(blurGc, blurLayer, blurFrame, precompLayersByIndex);
                        transformApplier.applyLayerTransform(blurGc, blurLayer, blurFrame);
                        // Render content
                        renderPrecompLayerContentOnly(blurGc, blurLayer, blurFrame, assetsById, animation, layerActivityEvaluator, solidColorLayerRenderer, shapeGroupRenderer, shapeRendererDelegate);
                    });
            return;
        }

        renderPrecompLayerInternal(gc, layer, frame, precompLayersByIndex, assetsById, animation, precompWidth, precompHeight, layerActivityEvaluator, solidColorLayerRenderer, shapeGroupRenderer, shapeRendererDelegate);
    }

    /**
     * Internal rendering method for precomp layers (without blur handling).
     *
     * @param gc                      graphics context
     * @param layer                   precomp child layer
     * @param frame                   local precomp frame
     * @param precompLayersByIndex    layer lookup map
     * @param assetsById              asset lookup map
     * @param animation               root animation
     * @param parentPrecompWidth      parent precomp width
     * @param parentPrecompHeight     parent precomp height
     * @param layerActivityEvaluator  layer activity callback
     * @param solidColorLayerRenderer solid color renderer callback
     * @param shapeGroupRenderer      shape group renderer callback
     * @param shapeRendererDelegate   shape renderer callback
     */
    private void renderPrecompLayerInternal(GraphicsContext gc,
                                            Layer layer,
                                            double frame,
                                            Map<Integer, Layer> precompLayersByIndex,
                                            Map<String, Asset> assetsById,
                                            Animation animation,
                                            double parentPrecompWidth,
                                            double parentPrecompHeight,
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

        applyPrecompParentTransforms(gc, layer, frame, precompLayersByIndex);
        transformApplier.applyLayerTransform(gc, layer, frame);

        if (layer.layerType() == LayerType.PRECOMPOSITION) {
            renderPrecompositionLayer(gc,
                    layer,
                    frame,
                    assetsById,
                    animation,
                    parentPrecompWidth,
                    parentPrecompHeight,
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
            renderPrecompShapes(gc, layer, frame, shapeGroupRenderer, shapeRendererDelegate);
        } else {
            logger.debug("Skipping shape rendering for NULL layer: {}", layer.name());
        }

        gc.restore();
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
                                     ShapeRendererDelegate shapeRendererDelegate) {
        if (layer.shapes() == null || layer.shapes().isEmpty()) {
            logger.debug("Layer has no shapes");
            return;
        }

        logger.debug("Layer has {} shapes", layer.shapes().size());

        TrimPath layerTrimPath = null;
        for (BaseShape shape : layer.shapes()) {
            if (shape instanceof TrimPath trim) {
                layerTrimPath = trim;
                logger.debug("Found layer-level TrimPath");
            }
        }

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
     * @param assetsById              asset lookup map
     * @param animation               root animation
     * @param layerActivityEvaluator  layer activity callback
     * @param solidColorLayerRenderer solid color renderer callback
     * @param shapeGroupRenderer      shape group renderer callback
     * @param shapeRendererDelegate   shape renderer callback
     */
    private void renderPrecompLayerContentOnly(GraphicsContext gc,
                                               Layer layer,
                                               double frame,
                                               Map<String, Asset> assetsById,
                                               Animation animation,
                                               LayerActivityEvaluator layerActivityEvaluator,
                                               SolidColorLayerRenderer solidColorLayerRenderer,
                                               ShapeGroupRenderer shapeGroupRenderer,
                                               ShapeRendererDelegate shapeRendererDelegate) {
        logger.debug("renderPrecompLayerContentOnly called for {} type={}", layer.name(), layer.layerType());
        if (layer.layerType() == LayerType.PRECOMPOSITION) {
            logger.warn("Nested precomp blur not supported for content-only rendering: {}", layer.name());
            return;
        } else if (layer.layerType() == LayerType.IMAGE) {
            imageRenderer.render(gc, layer, animation);
        } else if (layer.layerType() == LayerType.SOLD_COLOR) {
            solidColorLayerRenderer.render(gc, layer);
        } else if (layer.layerType() == LayerType.TEXT) {
            textRenderer.render(gc, layer, frame);
        } else if (layer.layerType() != LayerType.NULL) {
            logger.debug("Rendering shapes for layer {} - has {} shapes", layer.name(),
                    layer.shapes() != null ? layer.shapes().size() : 0);
            renderPrecompShapes(gc, layer, frame, shapeGroupRenderer, shapeRendererDelegate);
        } else {
            logger.debug("Skipping shape rendering for NULL layer: {}", layer.name());
        }
    }

    /**
     * Recursively applies parent transforms for a precomp child layer.
     *
     * @param gc                   destination graphics context
     * @param layer                child layer whose parent chain is applied
     * @param frame                local precomp frame
     * @param precompLayersByIndex parent lookup map for the current precomp
     */
    private void applyPrecompParentTransforms(GraphicsContext gc,
                                              Layer layer,
                                              double frame,
                                              Map<Integer, Layer> precompLayersByIndex) {
        if (layer.indexParent() == null) {
            return;
        }

        Layer parent = precompLayersByIndex.get(layer.indexParent());
        if (parent == null) {
            logger.warn("Parent layer not found in precomp: {}", layer.indexParent());
            return;
        }

        applyPrecompParentTransforms(gc, parent, frame, precompLayersByIndex);
        transformApplier.applyLayerTransformWithoutOpacity(gc, parent, frame);
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

        // Asset height is currently modeled as String; parse when numeric.
        if (asset.height() != null) {
            try {
                double parsed = Double.parseDouble(asset.height());
                if (parsed > 0) {
                    return parsed;
                }
            } catch (NumberFormatException ex) {
                logger.debug("Unable to parse precomp asset height '{}' for {}", asset.height(), asset.id());
            }
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
}



