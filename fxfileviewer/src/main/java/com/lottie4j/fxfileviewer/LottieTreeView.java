package com.lottie4j.fxfileviewer;

import com.lottie4j.core.model.Animation;
import com.lottie4j.core.model.PropertyLabelValue;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableView;

import java.util.List;
import java.util.logging.Logger;

public class LottieTreeView extends TreeTableView<LottieTreeView.LottieProperty> {

    private static final Logger logger = Logger.getLogger(LottieTreeView.class.getName());

    private final String fileName;
    private final Animation animation;

    public LottieTreeView(String fileName, Animation animation) {
        this.fileName = fileName;
        this.animation = animation;

        TreeTableColumn<LottieProperty, String> treeTableColumn1 = new TreeTableColumn<>("Property");
        treeTableColumn1.setCellValueFactory(cellData -> cellData.getValue().getValue().getLabelProperty());
        treeTableColumn1.setMinWidth(250);
        this.getColumns().add(treeTableColumn1);

        TreeTableColumn<LottieProperty, String> treeTableColumn2 = new TreeTableColumn<>("Value");
        treeTableColumn2.setCellValueFactory(cellData -> cellData.getValue().getValue().getValueProperty());
        treeTableColumn2.setMinWidth(250);
        this.getColumns().add(treeTableColumn2);

        buildTree();
    }

    private void buildTree() {
        var rootItem = getLottiePropertyTreeItem("File", fileName);
        rootItem.setExpanded(true);

        var animationItem = getLottiePropertyTreeItem("Animation", (animation.name() == null ? "No name" : animation.name()));
        animationItem.setExpanded(true);
        rootItem.getChildren().add(animationItem);

        addLabelValues(animationItem, animation.getLabelValues());

        this.setRoot(rootItem);
    }

    private void addLabelValues(TreeItem<LottieProperty> parent, List<PropertyLabelValue> labelValues) {
        for (PropertyLabelValue labelValue : labelValues) {
            var treeItem = getLottiePropertyTreeItem(labelValue.label(), labelValue.value());
            if (labelValue.nestedLabelValues() != null && !labelValue.nestedLabelValues().isEmpty()) {
                addLabelValues(treeItem, labelValue.nestedLabelValues());
            }
            parent.getChildren().add(treeItem);
        }
    }

    private TreeItem<LottieProperty> getLottiePropertyTreeItem(String label, String value) {
        return new TreeItem<>(new LottieProperty(label, value));
    }

    public static class LottieProperty {

        private final StringProperty label;
        private final StringProperty value;

        public LottieProperty(String label, String value) {
            this.label = new SimpleStringProperty(label);
            this.value = new SimpleStringProperty(value);
        }

        public LottieProperty(PropertyLabelValue propertyLabelValue) {
            this.label = new SimpleStringProperty(propertyLabelValue.label());
            this.value = new SimpleStringProperty(propertyLabelValue.value());
        }

        public StringProperty getLabelProperty() {
            return label;
        }

        public StringProperty getValueProperty() {
            return value;
        }
    }
}
