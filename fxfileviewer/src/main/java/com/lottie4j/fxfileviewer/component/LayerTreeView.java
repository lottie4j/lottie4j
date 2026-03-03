package com.lottie4j.fxfileviewer.component;

import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.geometry.Insets;

import java.util.*;
import java.util.logging.Logger;

/**
 * TreeView for Lottie layers with visibility checkboxes and thumbnail previews.
 * Allows selective rendering of layers for debugging purposes.
 */
public class LayerTreeView extends TreeView<LayerTreeView.LayerNode> {

    private static final Logger logger = Logger.getLogger(LayerTreeView.class.getName());

    private final Animation animation;
    private final LottiePlayer lottiePlayer;
    private final Map<Integer, BooleanProperty> layerVisibilityMap;
    private final Map<Layer, Stage> previewWindows = new HashMap<>();
    private double currentFrame = 0;
    private Color backgroundColor = Color.WHITE;

    public LayerTreeView(Animation animation, LottiePlayer lottiePlayer) {
        this.animation = animation;
        this.lottiePlayer = lottiePlayer;
        this.layerVisibilityMap = new HashMap<>();

        buildTree();
        setupCellFactory();
    }

    private void buildTree() {
        LayerNode rootNode = new LayerNode("Layers", null, LayerNodeType.ROOT);
        TreeItem<LayerNode> rootItem = new TreeItem<>(rootNode);
        rootItem.setExpanded(true);

        if (animation.layers() != null) {
            // Build layer items in reverse order (Lottie renders bottom-to-top)
            List<Layer> layers = new ArrayList<>(animation.layers());
            Collections.reverse(layers);

            for (Layer layer : layers) {
                BooleanProperty visible = new SimpleBooleanProperty(true);
                Integer layerIndex = layer.indexLayer() != null ? layer.indexLayer().intValue() : layers.indexOf(layer);
                layerVisibilityMap.put(layerIndex, visible);

                LayerNode layerNode = new LayerNode(
                    layer.name() != null ? layer.name() : "Layer " + layerIndex,
                    layer,
                    LayerNodeType.LAYER
                );
                layerNode.setVisibleProperty(visible);

                TreeItem<LayerNode> layerItem = new TreeItem<>(layerNode);

                // Add layer info as children
                addLayerInfo(layerItem, layer, layerIndex);

                rootItem.getChildren().add(layerItem);
            }
        }

        setRoot(rootItem);
    }

    private void addLayerInfo(TreeItem<LayerNode> parent, Layer layer, int layerIndex) {
        // Add track matte info
        if (layer.matteMode() != null) {
            String matteInfo = "Track Matte: " + layer.matteMode() +
                             " (matte layer: " + (layerIndex - 1) + ")";
            LayerNode infoNode = new LayerNode(matteInfo, null, LayerNodeType.INFO);
            parent.getChildren().add(new TreeItem<>(infoNode));
        }

        if (layer.matteTarget() != null && layer.matteTarget() == 1) {
            LayerNode infoNode = new LayerNode("Is Matte Source", null, LayerNodeType.INFO);
            parent.getChildren().add(new TreeItem<>(infoNode));
        }

        // Add parent info
        if (layer.indexParent() != null) {
            LayerNode infoNode = new LayerNode("Parent: Layer " + layer.indexParent(), null, LayerNodeType.INFO);
            parent.getChildren().add(new TreeItem<>(infoNode));
        }

        // Add layer type
        String typeInfo = "Type: " + layer.layerType();
        LayerNode typeNode = new LayerNode(typeInfo, null, LayerNodeType.INFO);
        parent.getChildren().add(new TreeItem<>(typeNode));

        // Add shape count
        if (layer.shapes() != null && !layer.shapes().isEmpty()) {
            String shapeInfo = "Shapes: " + layer.shapes().size();
            LayerNode shapeNode = new LayerNode(shapeInfo, null, LayerNodeType.INFO);
            parent.getChildren().add(new TreeItem<>(shapeNode));
        }
    }

    private void setupCellFactory() {
        setCellFactory(tv -> new LayerTreeCell());
    }

    /**
     * Get set of visible layer indices
     */
    public Set<Integer> getVisibleLayerIndices() {
        Set<Integer> visibleIndices = new HashSet<>();

        for (Map.Entry<Integer, BooleanProperty> entry : layerVisibilityMap.entrySet()) {
            if (entry.getValue().get()) {
                visibleIndices.add(entry.getKey());

                // Auto-include matte source if layer uses track matte
                Layer layer = getLayerByIndex(entry.getKey());
                if (layer != null && layer.matteMode() != null) {
                    // Matte source is typically the previous layer (index - 1)
                    visibleIndices.add(entry.getKey() - 1);
                    logger.fine("Auto-including matte source layer " + (entry.getKey() - 1) +
                              " for layer " + entry.getKey());
                }
            }
        }

        return visibleIndices;
    }

    /**
     * Set current frame for thumbnail generation and update all open preview windows
     */
    public void setCurrentFrame(double frame) {
        this.currentFrame = frame;
        // Update all open preview windows
        updatePreviewWindows();
    }

    /**
     * Set background color for thumbnail generation and update all open preview windows
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        // Update all open preview windows
        updatePreviewWindows();
    }

    /**
     * Update all open preview windows with current frame and background color
     */
    private void updatePreviewWindows() {
        for (Map.Entry<Layer, Stage> entry : previewWindows.entrySet()) {
            Stage stage = entry.getValue();
            if (stage.isShowing()) {
                // Find the ImageView in the stage and update it
                Scene scene = stage.getScene();
                if (scene != null && scene.getRoot() instanceof VBox) {
                    VBox root = (VBox) scene.getRoot();
                    if (!root.getChildren().isEmpty() && root.getChildren().get(0) instanceof Label) {
                        Label headerLabel = (Label) root.getChildren().get(0);
                        Integer layerIndex = entry.getKey().indexLayer() != null ?
                            entry.getKey().indexLayer().intValue() : 0;
                        headerLabel.setText("Layer " + layerIndex + " at frame " + (int)currentFrame);
                    }
                    if (root.getChildren().size() > 1 && root.getChildren().get(1) instanceof ImageView) {
                        ImageView imageView = (ImageView) root.getChildren().get(1);
                        WritableImage newImage = generateLayerThumbnail(entry.getKey(), 200);
                        imageView.setImage(newImage);
                    }
                }
            }
        }
    }

    private Layer getLayerByIndex(int index) {
        if (animation.layers() == null) return null;
        return animation.layers().stream()
            .filter(l -> l.indexLayer() != null && l.indexLayer().intValue() == index)
            .findFirst()
            .orElse(null);
    }

    /**
     * Generate thumbnail for a layer at current frame
     */
    private WritableImage generateLayerThumbnail(Layer layer, int size) {
        // Render single layer
        Set<Integer> singleLayer = new HashSet<>();
        Integer layerIndex = layer.indexLayer() != null ? layer.indexLayer().intValue() : 0;
        singleLayer.add(layerIndex);

        // Include matte source if needed
        if (layer.matteMode() != null) {
            singleLayer.add(layerIndex - 1);
        }

        // Create temporary player canvas for rendering - LottiePlayer extends Canvas
        LottiePlayer tempPlayer = new LottiePlayer(animation, size, size);
        tempPlayer.setBackgroundColor(backgroundColor);
        tempPlayer.render(currentFrame, singleLayer);

        // Take snapshot of the player canvas
        return tempPlayer.snapshot(null, null);
    }

    /**
     * Node types in the tree
     */
    public enum LayerNodeType {
        ROOT,
        LAYER,
        INFO
    }

    /**
     * Data model for tree nodes
     */
    public static class LayerNode {
        private final String name;
        private final Layer layer;
        private final LayerNodeType type;
        private BooleanProperty visible;

        public LayerNode(String name, Layer layer, LayerNodeType type) {
            this.name = name;
            this.layer = layer;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public Layer getLayer() {
            return layer;
        }

        public LayerNodeType getType() {
            return type;
        }

        public BooleanProperty getVisibleProperty() {
            return visible;
        }

        public void setVisibleProperty(BooleanProperty visible) {
            this.visible = visible;
        }
    }

    /**
     * Custom cell for rendering layer nodes with checkboxes and thumbnails
     */
    class LayerTreeCell extends TreeCell<LayerNode> {

        private CheckBox checkBox;
        private Label label;
        private Button previewButton;

        public LayerTreeCell() {
            checkBox = new CheckBox();
            label = new Label();
            previewButton = new Button("🔍");
            previewButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 5 2 5;");

            previewButton.setOnAction(e -> {
                LayerNode node = getItem();
                if (node != null && node.getLayer() != null) {
                    showLayerPreview(node.getLayer());
                }
            });
        }

        @Override
        protected void updateItem(LayerNode item, boolean empty) {
            super.updateItem(item, empty);

            if (empty || item == null) {
                setText(null);
                setGraphic(null);
            } else {
                if (item.getType() == LayerNodeType.LAYER) {
                    // Layer node with checkbox and preview button
                    checkBox.setSelected(item.getVisibleProperty().get());
                    checkBox.selectedProperty().bindBidirectional(item.getVisibleProperty());

                    // Warn if layer is a matte source
                    if (item.getLayer().matteTarget() != null && item.getLayer().matteTarget() == 1) {
                        label.setText(item.getName() + " (matte source)");
                        label.setStyle("-fx-text-fill: #0066cc;");
                    } else {
                        label.setText(item.getName());
                        label.setStyle("");
                    }

                    HBox box = new HBox(5, checkBox, label, previewButton);
                    setGraphic(box);
                    setText(null);
                } else if (item.getType() == LayerNodeType.INFO) {
                    // Info node - just text
                    setText(null);
                    label.setText(item.getName());
                    label.setStyle("-fx-text-fill: #666666; -fx-font-size: 11px;");
                    setGraphic(label);
                } else {
                    // Root node
                    setText(item.getName());
                    setGraphic(null);
                }
            }
        }

        private void showLayerPreview(Layer layer) {
            // Check if preview window already exists for this layer
            if (previewWindows.containsKey(layer) && previewWindows.get(layer).isShowing()) {
                // Bring existing window to front
                previewWindows.get(layer).toFront();
                return;
            }

            // Create preview window
            Stage previewStage = new Stage();
            previewStage.setTitle("Layer Preview: " + layer.name());

            Integer layerIndex = layer.indexLayer() != null ? layer.indexLayer().intValue() : 0;
            Label headerLabel = new Label("Layer " + layerIndex + " at frame " + (int)currentFrame);
            headerLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

            // Generate thumbnail (larger for preview)
            WritableImage thumbnail = generateLayerThumbnail(layer, 200);
            ImageView imageView = new ImageView(thumbnail);
            imageView.setPreserveRatio(true);

            VBox content = new VBox(10, headerLabel, imageView);
            content.setPadding(new Insets(10));
            content.setStyle("-fx-alignment: center;");

            Scene scene = new Scene(content);
            previewStage.setScene(scene);

            // Remove from map when window is closed
            previewStage.setOnCloseRequest(e -> previewWindows.remove(layer));

            // Store window reference
            previewWindows.put(layer, previewStage);

            // Show window (non-modal)
            previewStage.show();
        }
    }
}
