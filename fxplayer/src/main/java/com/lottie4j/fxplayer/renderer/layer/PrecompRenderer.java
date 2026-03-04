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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PrecompRenderer {

    private static final Logger logger = Logger.getLogger(PrecompRenderer.class.getName());

    private final TransformApplier transformApplier;
    private final TextRenderer textRenderer;
    private final ImageRenderer imageRenderer;

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
        if (layer.referenceId() == null) {
            logger.warning("Precomposition layer has no referenceId: " + layer.name());
            return;
        }

        Asset asset = assetsById.get(layer.referenceId());
        if (asset == null) {
            logger.warning("Asset not found for referenceId: " + layer.referenceId());
            return;
        }

        if (asset.layers() == null || asset.layers().isEmpty()) {
            logger.warning("Precomposition asset has no layers: " + layer.referenceId());
            return;
        }

        double localFrame = FrameTiming.toLocalFrame(layer, frame);
        logger.finer("Rendering precomposition: " + layer.referenceId() + " with " + asset.layers().size()
                + " layers at localFrame=" + localFrame + " (global=" + frame + ")");

        Map<Integer, Layer> precompLayersByIndex = new HashMap<>();
        for (Layer precompLayer : asset.layers()) {
            if (precompLayer.indexLayer() != null) {
                precompLayersByIndex.put(precompLayer.indexLayer().intValue(), precompLayer);
            }
        }

        for (int i = asset.layers().size() - 1; i >= 0; i--) {
            Layer precompLayer = asset.layers().get(i);
            if (!layerActivityEvaluator.isActive(precompLayer, localFrame)) {
                continue;
            }
            logger.fine("Rendering precomp layer [" + i + "] ind=" + precompLayer.indexLayer()
                    + " name='" + precompLayer.name() + "'");
            renderPrecompLayer(gc,
                    precompLayer,
                    localFrame,
                    precompLayersByIndex,
                    assetsById,
                    animation,
                    layerActivityEvaluator,
                    solidColorLayerRenderer,
                    shapeGroupRenderer,
                    shapeRendererDelegate);
        }
    }

    /**
     * Renders a single layer inside a precomposition, including recursive nested precomps.
     */
    private void renderPrecompLayer(GraphicsContext gc,
                                    Layer layer,
                                    double frame,
                                    Map<Integer, Layer> precompLayersByIndex,
                                    Map<String, Asset> assetsById,
                                    Animation animation,
                                    LayerActivityEvaluator layerActivityEvaluator,
                                    SolidColorLayerRenderer solidColorLayerRenderer,
                                    ShapeGroupRenderer shapeGroupRenderer,
                                    ShapeRendererDelegate shapeRendererDelegate) {
        gc.save();

        applyPrecompParentTransforms(gc, layer, frame, precompLayersByIndex);
        transformApplier.applyLayerTransform(gc, layer, frame);

        if (layer.layerType() == LayerType.PRECOMPOSITION) {
            renderPrecompositionLayer(gc,
                    layer,
                    frame,
                    assetsById,
                    animation,
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
            renderPrecompShapes(layer, frame, shapeGroupRenderer, shapeRendererDelegate);
        } else {
            logger.finer("Skipping shape rendering for NULL layer: " + layer.name());
        }

        gc.restore();
    }

    /**
     * Renders shape content for a non-null precomp layer.
     *
     * @param layer                 precomp child layer containing shapes
     * @param frame                 local precomp frame
     * @param shapeGroupRenderer    callback used for group shapes
     * @param shapeRendererDelegate callback used for direct shape primitives
     */
    private void renderPrecompShapes(Layer layer,
                                     double frame,
                                     ShapeGroupRenderer shapeGroupRenderer,
                                     ShapeRendererDelegate shapeRendererDelegate) {
        if (layer.shapes() == null || layer.shapes().isEmpty()) {
            logger.finer("Layer has no shapes");
            return;
        }

        logger.fine("Layer has " + layer.shapes().size() + " shapes");

        TrimPath layerTrimPath = null;
        for (BaseShape shape : layer.shapes()) {
            if (shape instanceof TrimPath trim) {
                layerTrimPath = trim;
                logger.finer("Found layer-level TrimPath");
            }
        }

        List<BaseShape> shapes = layer.shapes();
        for (int i = shapes.size() - 1; i >= 0; i--) {
            BaseShape shape = shapes.get(i);
            if (shape instanceof TrimPath) {
                continue;
            }

            switch (shape.type().shapeGroup()) {
                case GROUP -> shapeGroupRenderer.render(shape, frame, layerTrimPath);
                case SHAPE -> shapeRendererDelegate.render(shape, null, frame);
                case STYLE -> {
                    // Styles are consumed by parent group renderers.
                }
                default -> logger.finer("Unsupported shape type: " + shape.type().shapeGroup());
            }
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
            logger.warning("Parent layer not found in precomp: " + layer.indexParent());
            return;
        }

        applyPrecompParentTransforms(gc, parent, frame, precompLayersByIndex);
        transformApplier.applyLayerTransformWithoutOpacity(gc, parent, frame);
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
         * @param shape         group shape item
         * @param frame         frame to sample
         * @param layerTrimPath optional trim path inherited from layer context
         */
        void render(BaseShape shape, double frame, TrimPath layerTrimPath);
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

