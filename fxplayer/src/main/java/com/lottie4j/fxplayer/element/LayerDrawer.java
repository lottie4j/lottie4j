package com.lottie4j.fxplayer.element;

import com.lottie4j.core.model.Layer;
import com.lottie4j.core.model.shape.Group;
import javafx.scene.canvas.GraphicsContext;

import java.util.logging.Logger;

public class LayerDrawer {

    private final Logger logger = Logger.getLogger(LayerDrawer.class.getName());

    private final GraphicsContext gc;
    private final Layer layer;

    public LayerDrawer(GraphicsContext gc, Layer layer) {
        this.gc = gc;
        this.layer = layer;

        if (layer.shapes() != null) {
            layer.shapes().forEach(s -> {
                if (s instanceof Group group) {
                    var groupDrawer = new GroupDrawer(gc, group, layer);
                    //groupDrawers.add(groupDrawer);
                }
            });
        }
    }
}
