package com.lottie4j.fxfileviewer.component;

import com.lottie4j.core.info.PropertyLabelValue;
import com.lottie4j.core.info.PropertyListingList;
import com.lottie4j.core.model.Animation;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

/**
 * Tree table view component for displaying Lottie animation properties in a hierarchical structure.
 * Shows animation metadata including name, dimensions, layers, and nested property values.
 */
public class LottieTreeView extends TreeTableView<LottieTreeView.LottieProperty> {

    private final String fileName;
    private final Animation animation;

    /**
     * Creates a tree table view for the given Lottie animation.
     *
     * @param fileName  name of the loaded animation file
     * @param animation parsed animation model
     */
    public LottieTreeView(String fileName, Animation animation) {
        this.fileName = fileName;
        this.animation = animation;

        TreeTableColumn<LottieProperty, String> treeTableColumn1 = new TreeTableColumn<>("Property");
        treeTableColumn1.setCellValueFactory(cellData -> cellData.getValue().getValue().getLabelProperty());
        treeTableColumn1.setMinWidth(250);
        this.getColumns().add(treeTableColumn1);

        TreeTableColumn<LottieProperty, String> treeTableColumn2 = new TreeTableColumn<>("Value");
        treeTableColumn2.setCellValueFactory(cellData -> cellData.getValue().getValue().getValueProperty());
        treeTableColumn2.setMinWidth(150);
        this.getColumns().add(treeTableColumn2);

        buildTree();
    }

    /**
     * Builds the hierarchical tree structure from animation properties.
     */
    private void buildTree() {
        var rootItem = getLottiePropertyTreeItem("File", fileName);
        rootItem.setExpanded(true);

        var animationItem = getLottiePropertyTreeItem("Animation", (animation.name() == null ? "No name" : animation.name()));
        animationItem.setExpanded(true);
        rootItem.getChildren().add(animationItem);

        addLabelValues(animationItem, animation.getList());

        this.setRoot(rootItem);
    }

    /**
     * Recursively adds nested label-value properties to the tree.
     *
     * @param parent parent tree item to add children to
     * @param list   property listing containing label-value pairs
     */
    private void addLabelValues(TreeItem<LottieProperty> parent, PropertyListingList list) {
        for (PropertyLabelValue labelValue : list.getList()) {
            var treeItem = getLottiePropertyTreeItem(labelValue.label(), labelValue.value());
            if (labelValue.nestedLabelValues().isPresent() && !labelValue.nestedLabelValues().get().getList().isEmpty()) {
                addLabelValues(treeItem, labelValue.nestedLabelValues().get());
            }
            parent.getChildren().add(treeItem);
        }
    }

    /**
     * Creates a tree item wrapper for a label-value pair.
     *
     * @param label property label
     * @param value property value
     * @return tree item containing the property
     */
    private TreeItem<LottieProperty> getLottiePropertyTreeItem(String label, String value) {
        return new TreeItem<>(new LottieProperty(label, value));
    }

    /**
     * Data model for a Lottie property entry with label and value.
     */
    public static class LottieProperty {

        private final StringProperty label;
        private final StringProperty value;

        /**
         * Creates a property entry with label and value.
         *
         * @param label property label
         * @param value property value
         */
        public LottieProperty(String label, String value) {
            this.label = new SimpleStringProperty(label);
            this.value = new SimpleStringProperty(value);
        }

        /**
         * Gets the label property for JavaFX binding.
         *
         * @return observable string property for the label
         */
        public StringProperty getLabelProperty() {
            return label;
        }

        /**
         * Gets the value property for JavaFX binding.
         *
         * @return observable string property for the value
         */
        public StringProperty getValueProperty() {
            return value;
        }
    }
}
