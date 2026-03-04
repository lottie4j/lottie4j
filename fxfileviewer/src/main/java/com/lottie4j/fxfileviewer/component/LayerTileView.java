package com.lottie4j.fxfileviewer.component;

import com.lottie4j.core.file.LottieFileSaver;
import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.Layer;
import com.lottie4j.fxplayer.LottiePlayer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;

/**
 * Tile view for Lottie layers with live-updating thumbnails.
 * Shows all layers as tiles with real-time preview updates during playback.
 */
public class LayerTileView extends ScrollPane {

    private static final Logger logger = LoggerFactory.getLogger(LayerTileView.class);
    private static final int TILE_SIZE = 150;
    private static final int TILE_SPACING = 10;

    private final Animation animation;
    private final Map<Integer, LayerTile> layerTiles;
    private final Map<Integer, BooleanProperty> layerVisibilityMap;
    private final DoubleProperty frameProperty;
    private Color backgroundColor = Color.WHITE;
    private double currentFrame = 0;
    private boolean liveUpdatesEnabled = false;
    private VBox contentWrapper;
    private Button enableButton;

    /**
     * Creates a tile view for the animation layers.
     *
     * @param animation     animation containing layers to display
     * @param frameProperty observable frame property to monitor for updates
     */
    public LayerTileView(Animation animation, DoubleProperty frameProperty) {
        this.animation = animation;
        this.layerTiles = new HashMap<>();
        this.layerVisibilityMap = new HashMap<>();
        this.frameProperty = frameProperty;

        // Listen to frame changes and update all tiles (only if enabled)
        frameProperty.addListener((obs, oldVal, newVal) -> {
            currentFrame = newVal.doubleValue();
            if (liveUpdatesEnabled) {
                updateAllTiles();
            }
        });

        buildTileView();
        setupScrollPane();
    }

    /**
     * Configures the scroll pane appearance and behavior.
     */
    private void setupScrollPane() {
        setFitToWidth(true);
        setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        setStyle("-fx-background-color: #f5f5f5;");
    }

    /**
     * Builds the tile grid layout with an Enable button and layer tiles.
     */
    private void buildTileView() {
        contentWrapper = new VBox(10);
        contentWrapper.setPadding(new Insets(TILE_SPACING));
        contentWrapper.setStyle("-fx-background-color: #f5f5f5;");

        // Create enable/disable button
        enableButton = new Button("Enable Live Updates");
        enableButton.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        enableButton.setOnAction(e -> toggleLiveUpdates());

        // Info label
        Label infoLabel = new Label("⚠️ Live updates can be slow with many layers. Click to enable.");
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666666;");

        HBox buttonBox = new HBox(10, enableButton, infoLabel);
        buttonBox.setAlignment(Pos.CENTER_LEFT);
        buttonBox.setPadding(new Insets(10));

        FlowPane flowPane = new FlowPane();
        flowPane.setHgap(TILE_SPACING);
        flowPane.setVgap(TILE_SPACING);
        flowPane.setPadding(new Insets(TILE_SPACING));
        flowPane.setStyle("-fx-background-color: #f5f5f5;");

        if (animation.layers() != null) {
            // Show layers in reverse order (Lottie renders bottom-to-top)
            List<Layer> layers = new ArrayList<>(animation.layers());
            Collections.reverse(layers);

            for (Layer layer : layers) {
                Integer layerIndex = layer.indexLayer() != null ? layer.indexLayer().intValue() : layers.indexOf(layer);

                BooleanProperty visible = new SimpleBooleanProperty(true);
                layerVisibilityMap.put(layerIndex, visible);

                LayerTile tile = new LayerTile(layer, layerIndex, visible);
                layerTiles.put(layerIndex, tile);

                flowPane.getChildren().add(tile);
            }
        }

        contentWrapper.getChildren().addAll(buttonBox, flowPane);
        setContent(contentWrapper);
    }

    /**
     * Toggle live updates on/off.
     */
    private void toggleLiveUpdates() {
        liveUpdatesEnabled = !liveUpdatesEnabled;

        if (liveUpdatesEnabled) {
            enableButton.setText("Disable Live Updates");
            enableButton.setStyle("-fx-font-weight: bold; -fx-font-size: 12px; -fx-background-color: #90EE90;");
            // Update once to show current frame
            updateAllTiles();
        } else {
            enableButton.setText("Enable Live Updates");
            enableButton.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        }
    }

    /**
     * Disable live updates (called when loading new file).
     */
    public void disableLiveUpdates() {
        if (liveUpdatesEnabled) {
            liveUpdatesEnabled = false;
            enableButton.setText("Enable Live Updates");
            enableButton.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        }
    }

    /**
     * Update all tiles with current frame.
     */
    private void updateAllTiles() {
        // Only update if enabled
        if (!liveUpdatesEnabled) {
            return;
        }

        // Run on JavaFX thread to avoid threading issues
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(this::updateAllTiles);
            return;
        }

        for (LayerTile tile : layerTiles.values()) {
            tile.updatePreview(currentFrame, backgroundColor);
        }
    }

    /**
     * Set background color and update all tiles.
     *
     * @param color new background color
     */
    public void setBackgroundColor(Color color) {
        this.backgroundColor = color;
        updateAllTiles();
    }

    /**
     * Get set of visible layer indices.
     *
     * @return set containing indices of visible layers, including matte sources
     */
    public Set<Integer> getVisibleLayerIndices() {
        Set<Integer> visibleIndices = new HashSet<>();

        for (Map.Entry<Integer, BooleanProperty> entry : layerVisibilityMap.entrySet()) {
            if (entry.getValue().get()) {
                visibleIndices.add(entry.getKey());

                // Auto-include matte source if layer uses track matte
                Layer layer = getLayerByIndex(entry.getKey());
                if (layer != null && layer.matteMode() != null) {
                    visibleIndices.add(entry.getKey() - 1);
                }
            }
        }

        return visibleIndices;
    }

    /**
     * Get the visibility map to add listeners.
     *
     * @return map of layer indices to visibility properties
     */
    public Map<Integer, BooleanProperty> getLayerVisibilityMap() {
        return layerVisibilityMap;
    }

    /**
     * Retrieves a layer by its index.
     *
     * @param index layer index to find
     * @return layer with matching index, or null if not found
     */
    private Layer getLayerByIndex(int index) {
        if (animation.layers() == null) return null;
        return animation.layers().stream()
                .filter(l -> l.indexLayer() != null && l.indexLayer().intValue() == index)
                .findFirst()
                .orElse(null);
    }

    /**
     * Individual layer tile with checkbox, name, and live preview.
     */
    private class LayerTile extends VBox {
        private final Layer layer;
        private final Integer layerIndex;
        private final BooleanProperty visible;
        private final ImageView previewImage;
        private final Label frameLabel;
        private final CheckBox visibilityCheckBox;

        /**
         * Creates a tile for a single layer.
         *
         * @param layer      layer to display
         * @param layerIndex index of the layer
         * @param visible    visibility property for binding
         */
        public LayerTile(Layer layer, Integer layerIndex, BooleanProperty visible) {
            this.layer = layer;
            this.layerIndex = layerIndex;
            this.visible = visible;

            setAlignment(Pos.TOP_CENTER);
            setSpacing(5);
            setPadding(new Insets(8));
            setStyle("-fx-background-color: white; " +
                    "-fx-border-color: #cccccc; " +
                    "-fx-border-width: 1; " +
                    "-fx-border-radius: 4; " +
                    "-fx-background-radius: 4;");
            setPrefWidth(TILE_SIZE);
            setMaxWidth(TILE_SIZE);

            // Layer name and checkbox
            HBox header = new HBox(5);
            header.setAlignment(Pos.CENTER_LEFT);

            visibilityCheckBox = new CheckBox();
            visibilityCheckBox.setSelected(true);
            visibilityCheckBox.selectedProperty().bindBidirectional(visible);

            String layerName = layer.name() != null ? layer.name() : "Layer " + layerIndex;
            Label nameLabel = new Label(layerName);
            nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px;");
            nameLabel.setMaxWidth(TILE_SIZE - 40);

            // Show matte source indicator
            if (layer.matteTarget() != null && layer.matteTarget() == 1) {
                nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 11px; -fx-text-fill: #0066cc;");
            }

            header.getChildren().addAll(visibilityCheckBox, nameLabel);

            // Preview image
            previewImage = new ImageView();
            previewImage.setFitWidth(TILE_SIZE - 16);
            previewImage.setFitHeight(TILE_SIZE - 16);
            previewImage.setPreserveRatio(true);

            // Frame number label
            frameLabel = new Label("Frame: 0");
            frameLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #666666;");

            // Layer info label
            String info = "Type: " + layer.layerType();
            if (layer.shapes() != null && !layer.shapes().isEmpty()) {
                info += " | Shapes: " + layer.shapes().size();
            }
            Label infoLabel = new Label(info);
            infoLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #999999;");
            infoLabel.setMaxWidth(TILE_SIZE - 16);

            // Save button
            Button saveButton = new Button("Save");
            saveButton.setStyle("-fx-font-size: 10px; -fx-padding: 4px 12px;");
            saveButton.setOnAction(e -> exportLayerAsJson());

            getChildren().addAll(header, previewImage, frameLabel, infoLabel, saveButton);

            // Generate initial preview
            updatePreview(currentFrame, backgroundColor);
        }

        /**
         * Update the preview for this tile.
         *
         * @param frame   current animation frame
         * @param bgColor background color for rendering
         */
        public void updatePreview(double frame, Color bgColor) {
            try {
                frameLabel.setText("Frame: " + (int) frame);

                // Generate thumbnail
                Set<Integer> singleLayer = new HashSet<>();
                singleLayer.add(layerIndex);

                // Include matte source if needed
                if (layer.matteMode() != null) {
                    singleLayer.add(layerIndex - 1);
                }

                // Create temporary player for rendering this layer
                LottiePlayer tempPlayer = new LottiePlayer(animation, TILE_SIZE - 16, TILE_SIZE - 16);
                tempPlayer.setBackgroundColor(bgColor);
                tempPlayer.render(frame, singleLayer);

                // Take snapshot
                WritableImage snapshot = tempPlayer.snapshot(null, null);
                previewImage.setImage(snapshot);

            } catch (Exception e) {
                logger.warn("Failed to update preview for layer {}: {}", layerIndex, e.getMessage());
            }
        }

        /**
         * Export this layer as a standalone JSON file.
         */
        private void exportLayerAsJson() {
            try {
                // Create file chooser
                FileChooser fileChooser = new FileChooser();
                fileChooser.setTitle("Save Layer as JSON");
                fileChooser.setInitialFileName(layer.name() + ".json");
                fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON files", "*.json"));

                // Get the stage from the scene
                Stage stage = (Stage) getScene().getWindow();
                File file = fileChooser.showSaveDialog(stage);

                if (file != null) {
                    // Export layer as JSON
                    LottieFileSaver.saveLayer(animation, layer, layerIndex, file);
                    logger.info("Layer exported to: {}", file.getAbsolutePath());
                }
            } catch (Exception e) {
                logger.error("Failed to export layer: {}", e.getMessage());
            }
        }
    }
}
